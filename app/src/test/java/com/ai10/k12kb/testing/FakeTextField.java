package com.ai10.k12kb.testing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;

import org.robolectric.RuntimeEnvironment;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * A text field as the IME sees it through its InputConnection.
 *
 * Edits follow android.view.inputmethod.BaseInputConnection, which EditText's
 * connection extends: commit/compose replace the composing span if there is one,
 * else the selection; deleteSurroundingText counts around the selection widened
 * to the composing span.
 *
 * Selection reports follow InputMethodManager.updateSelection: one report per
 * outermost edit (a whole batch counts as one), sent only when the selection or
 * composing span changed, carrying the previously reported selection as "old".
 * The app delivers them to the IME through the IME's main looper, so they arrive
 * after the IME returns from the call that caused them, never inside it.
 */
public final class FakeTextField implements InputConnection {

    /** Receives selection reports; in the harness this is the IME's onUpdateSelection. */
    public interface SelectionReceiver {
        void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                               int candidatesStart, int candidatesEnd);
    }

    private final SelectionReceiver receiver;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final StringBuilder text = new StringBuilder();
    private int selStart;
    private int selEnd;
    private int composingStart = -1;
    private int composingEnd = -1;

    private int batchDepth;
    /** {selStart, selEnd, composingStart, composingEnd} as last reported to the IME. */
    private int[] reported;
    private final ArrayDeque<int[]> pendingReports = new ArrayDeque<>();
    private boolean deferred;
    private boolean deliveryPosted;

    public FakeTextField(SelectionReceiver receiver) {
        this.receiver = receiver;
        this.reported = state();
    }

    // region Test API

    public String text() {
        return text.toString();
    }

    /**
     * The text with '|' at the cursor and '[' ']' around the composing span:
     * "하[나]|". A non-empty selection gets a '|' at each end.
     */
    public String shown() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            if (i == composingEnd) sb.append(']');
            if (i == selStart) sb.append('|');
            if (i == selEnd && selEnd != selStart) sb.append('|');
            if (i == composingStart) sb.append('[');
            if (i < text.length()) sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    /** The app replaces the field's content: composing is dropped and a report is always queued. */
    public void setText(String newText, int cursor) {
        text.setLength(0);
        text.append(newText);
        selStart = selEnd = clamp(cursor);
        clearComposing();
        queueReport(true);
    }

    /** While deferred, reports pile up until {@link #deliverReports()}. */
    public void deferReports(boolean defer) {
        deferred = defer;
        if (!defer && !pendingReports.isEmpty()) postDelivery();
    }

    /** Delivers every queued report now, in order. */
    public void deliverReports() {
        while (!pendingReports.isEmpty()) {
            int[] r = pendingReports.removeFirst();
            receiver.onUpdateSelection(r[0], r[1], r[2], r[3], r[4], r[5]);
        }
    }

    // endregion

    // region Editing (BaseInputConnection semantics)

    @Override
    public boolean commitText(CharSequence newText, int newCursorPosition) {
        beginEdit();
        replaceTarget(newText, newCursorPosition);
        clearComposing();
        endEdit();
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence newText, int newCursorPosition) {
        beginEdit();
        int start = replaceTarget(newText, newCursorPosition);
        if (newText.length() == 0) {
            clearComposing();
        } else {
            composingStart = start;
            composingEnd = start + newText.length();
        }
        endEdit();
        return true;
    }

    @Override
    public boolean finishComposingText() {
        beginEdit();
        clearComposing();
        endEdit();
        return true;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        beginEdit();
        int a = clamp(Math.min(start, end));
        int b = clamp(Math.max(start, end));
        if (a == b) {
            clearComposing();
        } else {
            composingStart = a;
            composingEnd = b;
        }
        endEdit();
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        beginEdit();
        int a = Math.min(selStart, selEnd);
        int b = Math.max(selStart, selEnd);
        if (composingStart != -1) {
            a = Math.min(a, composingStart);
            b = Math.max(b, composingEnd);
        }
        if (afterLength > 0) replace(b, Math.min(text.length(), b + afterLength), "");
        if (beforeLength > 0) replace(Math.max(0, a - beforeLength), a, "");
        endEdit();
        return true;
    }

    @Override
    public boolean setSelection(int start, int end) {
        if (start < 0 || end < 0 || start > text.length() || end > text.length()) return true;
        beginEdit();
        selStart = start;
        selEnd = end;
        endEdit();
        return true;
    }

    @Override
    public boolean beginBatchEdit() {
        beginEdit();
        return true;
    }

    @Override
    public boolean endBatchEdit() {
        endEdit();
        return batchDepth > 0;
    }

    // endregion

    // region Reading

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        int a = Math.min(selStart, selEnd);
        return text.substring(Math.max(0, a - n), a);
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        int b = Math.max(selStart, selEnd);
        return text.substring(b, Math.min(text.length(), b + n));
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        if (selStart == selEnd) return null;
        return text.substring(Math.min(selStart, selEnd), Math.max(selStart, selEnd));
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        return TextUtils.getCapsMode(text, Math.min(selStart, selEnd), reqModes);
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        return null;
    }

    // endregion

    // region Key events: what the app does with a key the IME did not consume

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        beginEdit();
        if (event.isCtrlPressed()) {
            // Ctrl+V pastes the clipboard at the cursor, as EditText does; other shortcuts do nothing here.
            if (event.getKeyCode() == KeyEvent.KEYCODE_V) replaceSelection(clipboardText());
            endEdit();
            return true;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL:
                if (selStart != selEnd) {
                    replaceSelection("");
                } else if (selStart > 0) {
                    replace(selStart - charCountBefore(selStart), selStart, "");
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (selStart != selEnd) {
                    selStart = selEnd = Math.min(selStart, selEnd);
                } else if (selStart > 0) {
                    selStart = selEnd = selStart - charCountBefore(selStart);
                }
                clearComposing();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (selStart != selEnd) {
                    selStart = selEnd = Math.max(selStart, selEnd);
                } else if (selEnd < text.length()) {
                    selStart = selEnd = selEnd + charCountAfter(selEnd);
                }
                clearComposing();
                break;
            case KeyEvent.KEYCODE_ENTER:
                replaceSelection("\n");
                break;
            default:
                int c = event.getUnicodeChar(event.getMetaState());
                if (c > 0) replaceSelection(new String(Character.toChars(c)));
                break;
        }
        endEdit();
        return true;
    }

    // endregion

    // region Not modelled

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        return false;
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        return false;
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        return false;
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        return false;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        return false;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        return false;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        return false;
    }

    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        return false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        return false;
    }

    @Override
    public Handler getHandler() {
        return null;
    }

    @Override
    public void closeConnection() {
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        return false;
    }

    // endregion

    // region Internals

    /**
     * Replaces the composing span, or the selection when there is none, and places
     * the cursor the way BaseInputConnection.replaceText does. Returns the start of
     * the inserted text.
     */
    private int replaceTarget(CharSequence newText, int newCursorPosition) {
        int a;
        int b;
        if (composingStart != -1) {
            a = composingStart;
            b = composingEnd;
        } else {
            a = Math.min(selStart, selEnd);
            b = Math.max(selStart, selEnd);
        }
        replace(a, b, newText);
        int cursor = newCursorPosition > 0
                ? a + newText.length() + newCursorPosition - 1
                : a + newCursorPosition;
        selStart = selEnd = clamp(cursor);
        return a;
    }

    private static CharSequence clipboardText() {
        ClipboardManager clipboard = (ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).getText();
        return text == null ? "" : text;
    }

    private void replaceSelection(CharSequence s) {
        replace(Math.min(selStart, selEnd), Math.max(selStart, selEnd), s);
    }

    /**
     * Replaces [start, end) and moves the selection and composing span the way
     * Editable spans move: selection points inside or at the edges of the replaced
     * range land after the new text; the composing span is exclusive at both ends,
     * so text inserted at its edges stays outside it.
     */
    private void replace(int start, int end, CharSequence s) {
        int len = s.length();
        int delta = len - (end - start);
        text.replace(start, end, s.toString());

        selStart = movePoint(selStart, start, end, len, delta);
        selEnd = movePoint(selEnd, start, end, len, delta);

        if (composingStart != -1) {
            int cs = composingStart < start ? composingStart
                    : composingStart >= end ? composingStart + delta
                    : start + len;
            int ce = composingEnd <= start ? composingEnd
                    : composingEnd >= end ? composingEnd + delta
                    : start;
            if (cs >= ce) {
                clearComposing();
            } else {
                composingStart = cs;
                composingEnd = ce;
            }
        }
    }

    private static int movePoint(int p, int start, int end, int len, int delta) {
        if (p < start) return p;
        if (p > end) return p + delta;
        return start + len;
    }

    private int charCountBefore(int index) {
        if (index >= 2 && Character.isSurrogatePair(text.charAt(index - 2), text.charAt(index - 1))) return 2;
        return 1;
    }

    private int charCountAfter(int index) {
        if (index + 1 < text.length() && Character.isSurrogatePair(text.charAt(index), text.charAt(index + 1))) return 2;
        return 1;
    }

    private void clearComposing() {
        composingStart = -1;
        composingEnd = -1;
    }

    private int clamp(int i) {
        return Math.max(0, Math.min(text.length(), i));
    }

    private int[] state() {
        return new int[]{selStart, selEnd, composingStart, composingEnd};
    }

    private void beginEdit() {
        batchDepth++;
    }

    private void endEdit() {
        if (batchDepth == 0) return;
        batchDepth--;
        if (batchDepth == 0) queueReport(false);
    }

    private void queueReport(boolean always) {
        int[] now = state();
        if (!always && Arrays.equals(now, reported)) return;
        pendingReports.addLast(new int[]{reported[0], reported[1], now[0], now[1], now[2], now[3]});
        reported = now;
        if (!deferred) postDelivery();
    }

    private void postDelivery() {
        if (deliveryPosted) return;
        deliveryPosted = true;
        mainHandler.post(() -> {
            deliveryPosted = false;
            if (!deferred) deliverReports();
        });
    }

    // endregion
}
