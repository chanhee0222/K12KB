package com.ai10.k12kb.hangul;

import java.util.ArrayDeque;

/**
 * Сеанс набора хангыля: единственный код, который пишет хангыль в редактор. Владеет
 * {@link HangulComposer} и знает режим показа: составляемый текст (обычные поля) или полоса над
 * клавиатурой (терминалы вроде Termux). В терминале ничто, однажды отправленное, не берётся назад:
 * закрытые слоги удерживаются, пока их ещё можно откатить, и уходят через {@code commit}.
 */
public final class HangulSession {
    /** Редактор, в который пишет сеанс. Один вызов сеанса = одна пара beginBatch/endBatch. */
    public interface Editor {
        void beginBatch();

        void endBatch();

        /** commitText(text, 1). */
        void commit(String text);

        /** setComposingText(text, 1); "" убирает текст составляемой области. */
        void setComposing(String text);

        /** finishComposingText(). */
        void finishComposing();

        /** deleteSurroundingText(count, 0). */
        void deleteBefore(int count);

        /**
         * Живая проверка редактора прямо сейчас: нет выделения, и текст перед курсором равен
         * {@code text}. Возвращает true, когда редактор не может ответить (чтение вернуло null).
         */
        boolean cursorRightAfter(String text);
    }

    /** Полоса над клавиатурой для терминалов; "" прячет её. */
    public interface PreeditStrip {
        void show(String text);
    }

    /** Сколько последних вводов можно откатить. */
    public static final int UNDO_DEPTH = 2;

    /** Что нужно, чтобы откатить один ввод. */
    private static final class UndoEntry {
        final int[] snapshot;
        final String preeditBefore;
        final String committed;

        UndoEntry(int[] snapshot, String preeditBefore, String committed) {
            this.snapshot = snapshot;
            this.preeditBefore = preeditBefore;
            this.committed = committed;
        }
    }

    private final Editor editor;
    private final PreeditStrip strip;
    private final HangulComposer composer = new HangulComposer();
    /** Старые записи в начале, последняя в конце. */
    private final ArrayDeque<UndoEntry> history = new ArrayDeque<>();
    private boolean terminal;

    public HangulSession(Editor editor, PreeditStrip strip) {
        this.editor = editor;
        this.strip = strip;
    }

    /** Переключает режим; если что-то набирается, сначала дописывает это в прежнем режиме. */
    public void setTerminalMode(boolean terminal) {
        if (isComposing()) {
            finish();
        }
        this.terminal = terminal;
    }

    public boolean isTerminalMode() {
        return terminal;
    }

    /** Набирается слог, либо в терминале есть удержанный текст. */
    public boolean isComposing() {
        return composer.isComposing() || !held().isEmpty();
    }

    public void input(char jamo) {
        SemanticJamo semantic = HangulJamo.fromChar(jamo);
        if (semantic == null) {
            throw new IllegalArgumentException("not a jamo: U+" + Integer.toHexString(jamo));
        }
        guardEditorHoldsSyllable();
        int[] snapshot = composer.snapshot();
        String preeditBefore = composer.preeditText();
        HangulComposer.Result result = composer.input(semantic);
        history.addLast(new UndoEntry(snapshot, preeditBefore, result.commit()));

        if (terminal) {
            // Запись, вышедшая за глубину отката, больше не может быть отменена: отправляем её.
            StringBuilder out = new StringBuilder();
            while (history.size() > UNDO_DEPTH) {
                out.append(history.removeFirst().committed);
            }
            commitBatch(out.toString());
            showStrip();
            return;
        }
        while (history.size() > UNDO_DEPTH) {
            history.removeFirst();
        }
        editor.beginBatch();
        if (!result.commit().isEmpty()) {
            editor.commit(result.commit());
        }
        editor.setComposing(result.preedit());
        editor.endBatch();
    }

    /** false: нечего удалять в сеансе, пусть удаляет редактор. */
    public boolean backspace() {
        if (guardEditorHoldsSyllable()) {
            return false;
        }
        HangulComposer.Result result = composer.backspace();
        if (terminal) {
            // История сбрасывается, значит удержанное больше не откатить: отправляем.
            String out = held();
            history.clear();
            commitBatch(out);
            showStrip();
        } else {
            history.clear();
        }
        if (result == null) {
            return false;
        }
        if (!terminal) {
            editor.beginBatch();
            editor.setComposing(result.preedit());
            if (!result.isComposing()) {
                editor.finishComposing();
            }
            editor.endBatch();
        }
        return true;
    }

    /** false: нет истории отката. */
    public boolean undoLastJamo() {
        if (guardEditorHoldsSyllable()) {
            return false;
        }
        if (history.isEmpty()) {
            return false;
        }
        UndoEntry entry = history.removeLast();
        composer.restore(entry.snapshot);
        if (terminal) {
            // Удержанное просто отбрасывается, редактору ничего не отправлено.
            showStrip();
            return true;
        }
        editor.beginBatch();
        if (!entry.committed.isEmpty()) {
            editor.setComposing("");
            editor.deleteBefore(entry.committed.length());
        }
        editor.setComposing(entry.preeditBefore);
        editor.endBatch();
        return true;
    }

    /** Дописывает всё отложенное, забывает историю, прячет полосу. */
    public void finish() {
        String pending = composer.flush();
        if (terminal) {
            commitBatch(held() + pending);
        } else if (!pending.isEmpty()) {
            editor.beginBatch();
            editor.finishComposing();
            editor.endBatch();
        }
        history.clear();
        strip.show("");
    }

    /**
     * Отчёт редактора о курсоре (onUpdateSelection). Отчёты бывают запоздалыми, поэтому сам по себе
     * отчёт ничего не меняет: он лишь повод сверить текущее состояние редактора.
     */
    public void onSelectionUpdate(int newSelStart, int newSelEnd, int composingStart, int composingEnd) {
        if (terminal || !isComposing()) {
            return;
        }
        if (composingStart != -1 && newSelStart == newSelEnd && newSelEnd == composingEnd) {
            return;
        }
        if (editor.cursorRightAfter(composer.preeditText())) {
            return; // отчёт устарел: курсор по-прежнему сразу за набираемым слогом
        }
        // Пользователь переставил курсор или приложение забрало текст; слог уже в редакторе.
        finish();
    }

    /**
     * Отчёт о курсоре может прийти позже следующей клавиши, поэтому перед правкой слога сверяем
     * редактор вживую. Если он больше не держит наш слог, дописываем и начинаем с чистого листа.
     * В терминале слог не в редакторе, там проверять нечего.
     *
     * @return true, если сеанс завершён из-за расхождения
     */
    private boolean guardEditorHoldsSyllable() {
        if (terminal || !composer.isComposing()) {
            return false;
        }
        if (editor.cursorRightAfter(composer.preeditText())) {
            return false;
        }
        finish();
        return true;
    }

    /** Забывает всё, ничего не пишет в редактор. */
    public void reset() {
        composer.reset();
        history.clear();
        strip.show("");
    }

    /** Текст закрытых слогов, ещё не отправленных в терминал. */
    private String held() {
        if (!terminal) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (UndoEntry entry : history) {
            sb.append(entry.committed);
        }
        return sb.toString();
    }

    private void showStrip() {
        strip.show(held() + composer.preeditText());
    }

    private void commitBatch(String text) {
        if (text.isEmpty()) {
            return;
        }
        editor.beginBatch();
        editor.commit(text);
        editor.endBatch();
    }
}
