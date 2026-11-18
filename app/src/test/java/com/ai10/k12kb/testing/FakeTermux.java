package com.ai10.k12kb.testing;

import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;

/**
 * A terminal as the IME sees it through its InputConnection, modelled on Termux's
 * TerminalView.onCreateInputConnection (a BaseInputConnection over a scratch editable):
 * <ul>
 *   <li>commitText sends the text to the terminal at once, replacing any held composing text;</li>
 *   <li>setComposingText only holds the text, finishComposingText sends what is held;</li>
 *   <li>deleteSurroundingText(n, 0) sends n DEL key events;</li>
 *   <li>a key event goes to the terminal as TerminalView.onKeyDown handles it: DEL erases the
 *       last character, a printable key types its character, Ctrl+letter sends the control
 *       code (Ctrl+C is U+0003); keys with no character send nothing here;</li>
 *   <li>nothing can be read back (getTextBeforeCursor is ""), and the terminal never reports
 *       a selection.</li>
 * </ul>
 * {@link #screen()} is everything the terminal received, after DEL erased from it.
 * {@link #dels()} counts the DELs it received, so a test can prove none was sent.
 */
public final class FakeTermux implements InputConnection {

    private final StringBuilder screen = new StringBuilder();
    private String held = "";
    private int dels;

    /** Everything the terminal received, with DELs applied. */
    public String screen() {
        return screen.toString();
    }

    /** How many DELs reached the terminal. */
    public int dels() {
        return dels;
    }

    // region Editing

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        held = "";
        screen.append(text);
        return true;
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        held = text.toString();
        return true;
    }

    @Override
    public boolean finishComposingText() {
        screen.append(held);
        held = "";
        return true;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        for (int i = 0; i < beforeLength; i++) {
            del();
        }
        return true;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            del();
            return true;
        }
        int meta = event.getMetaState() & ~KeyEvent.META_CTRL_MASK;
        int c = event.getUnicodeChar(meta);
        if (c <= 0) return true;
        if (event.isCtrlPressed()) {
            if (Character.isLetter(c)) screen.append((char) (Character.toLowerCase(c) & 0x1f));
        } else {
            screen.appendCodePoint(c);
        }
        return true;
    }

    @Override
    public boolean beginBatchEdit() {
        return true;
    }

    @Override
    public boolean endBatchEdit() {
        return true;
    }

    // endregion

    // region Reads: the terminal has no readable text

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        return "";
    }

    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        return "";
    }

    @Override
    public CharSequence getSelectedText(int flags) {
        return null;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        return 0;
    }

    @Override
    public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
        return null;
    }

    // endregion

    // region Not modelled

    @Override
    public boolean setComposingRegion(int start, int end) {
        return false;
    }

    @Override
    public boolean setSelection(int start, int end) {
        return false;
    }

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

    private void del() {
        dels++;
        if (screen.length() > 0) {
            int last = screen.length() - Character.charCount(screen.codePointBefore(screen.length()));
            screen.setLength(last);
        }
    }
}
