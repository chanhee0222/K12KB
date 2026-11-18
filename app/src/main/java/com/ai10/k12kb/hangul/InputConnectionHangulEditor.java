package com.ai10.k12kb.hangul;

import android.view.inputmethod.InputConnection;

import java.util.function.Supplier;

/**
 * {@link HangulSession.Editor} поверх текущего {@link InputConnection}. Соединение берётся заново
 * при каждом вызове: поле ввода меняется, а сеанс один на всё время жизни IME. Нет соединения —
 * запись молча пропускается.
 */
public final class InputConnectionHangulEditor implements HangulSession.Editor {
    private final Supplier<InputConnection> connection;

    public InputConnectionHangulEditor(Supplier<InputConnection> connection) {
        this.connection = connection;
    }

    @Override
    public void beginBatch() {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.beginBatchEdit();
        }
    }

    @Override
    public void endBatch() {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.endBatchEdit();
        }
    }

    @Override
    public void commit(String text) {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    @Override
    public void setComposing(String text) {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.setComposingText(text, 1);
        }
    }

    @Override
    public void finishComposing() {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.finishComposingText();
        }
    }

    @Override
    public void deleteBefore(int count) {
        InputConnection ic = connection.get();
        if (ic != null) {
            ic.deleteSurroundingText(count, 0);
        }
    }

    /**
     * getSelectedText отдаёт null и когда выделения нет, и когда редактор не отвечает; в обоих
     * случаях выделение не мешает. Null вместо текста перед курсором — редактор не отвечает: true.
     */
    @Override
    public boolean cursorRightAfter(String text) {
        InputConnection ic = connection.get();
        if (ic == null) {
            return true;
        }
        CharSequence selected = ic.getSelectedText(0);
        if (selected != null && selected.length() > 0) {
            return false;
        }
        CharSequence before = ic.getTextBeforeCursor(text.length(), 0);
        if (before == null) {
            return true;
        }
        return text.contentEquals(before);
    }
}
