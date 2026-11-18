package com.ai10.k12kb.hangul;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputContentInfo;

/**
 * Соединение с полем, которое перед каждой правкой текста или курсора сначала
 * выполняет beforeEdit — у клавиатуры это «дописать набираемый слог хангыля».
 *
 * Так все чужие правки (жесты, удаление слова, Ctrl-сочетания, вставка, голос,
 * плагины поиска, Enter) сначала закрывают слог, и их не нужно искать по одной.
 * Сам сеанс хангыля пишет мимо этой обёртки, напрямую в поле.
 *
 * Чтение, beginBatchEdit/endBatchEdit, clearMetaKeyStates и служебные вызовы
 * проходят как есть. Key-событие самой клавиши-модификатора (Ctrl, Shift, Alt…)
 * текст не правит и слог тоже не закрывает.
 */
public final class FinishingInputConnection extends InputConnectionWrapper {

    private final InputConnection target;
    private final Runnable beforeEdit;

    public FinishingInputConnection(InputConnection target, Runnable beforeEdit) {
        super(target, false);
        this.target = target;
        this.beforeEdit = beforeEdit;
    }

    /** Обёртка вокруг именно этого соединения. */
    public boolean wraps(InputConnection connection) {
        return target == connection;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        beforeEdit.run();
        return super.commitText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        beforeEdit.run();
        return super.setComposingText(text, newCursorPosition);
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        beforeEdit.run();
        return super.setComposingRegion(start, end);
    }

    @Override
    public boolean finishComposingText() {
        beforeEdit.run();
        return super.finishComposingText();
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        beforeEdit.run();
        return super.deleteSurroundingText(beforeLength, afterLength);
    }

    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        beforeEdit.run();
        return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength);
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (!KeyEvent.isModifierKey(event.getKeyCode()))
            beforeEdit.run();
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean setSelection(int start, int end) {
        beforeEdit.run();
        return super.setSelection(start, end);
    }

    @Override
    public boolean performContextMenuAction(int id) {
        beforeEdit.run();
        return super.performContextMenuAction(id);
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        beforeEdit.run();
        return super.performEditorAction(editorAction);
    }

    @Override
    public boolean commitCompletion(CompletionInfo text) {
        beforeEdit.run();
        return super.commitCompletion(text);
    }

    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        beforeEdit.run();
        return super.commitCorrection(correctionInfo);
    }

    @Override
    public boolean commitContent(InputContentInfo inputContentInfo, int flags, Bundle opts) {
        beforeEdit.run();
        return super.commitContent(inputContentInfo, flags, opts);
    }
}
