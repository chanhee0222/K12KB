package com.ai10.k12kb.hangul;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

/**
 * Редактор для тестов на чистой JVM: текст, выделение и составляемая область, как в текстовом поле.
 * Журнал {@link #calls} хранит вызовы ("commit:한", "compose:나", "del:1", "finish").
 * Запись допустима только внутри beginBatch/endBatch. С флагом {@link #terminal} тест падает при
 * setComposing, finishComposing и deleteBefore, как будто они ушли бы в терминал (DEL необратим).
 */
public final class RecordingEditor implements HangulSession.Editor {
    public final List<String> calls = new ArrayList<>();
    public boolean terminal;
    /** Сколько раз открывали пакет. */
    public int batchesOpened;
    /** Сколько раз сеанс читал редактор вживую (cursorRightAfter). */
    public int cursorChecks;

    private final StringBuilder text = new StringBuilder();
    private int selStart;
    private int selEnd;
    /** Составляемая область [compStart, compEnd); -1 если её нет. */
    private int compStart = -1;
    private int compEnd = -1;
    private int batchDepth;

    public String text() {
        return text.toString();
    }

    /** Конец выделения, он же курсор при свёрнутом выделении. */
    public int cursor() {
        return selEnd;
    }

    public boolean inBatch() {
        return batchDepth > 0;
    }

    /**
     * Текст с метками, как в FakeTextField: составляемая область в [ ], курсор |, выделение |abc|.
     * Пример: "한국[어]|".
     */
    public String shown() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            if (i == compEnd) {
                sb.append(']');
            }
            if (i == selStart) {
                sb.append('|');
            }
            if (i == selEnd && selEnd != selStart) {
                sb.append('|');
            }
            if (i == compStart) {
                sb.append('[');
            }
            if (i < text.length()) {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    @Override
    public void beginBatch() {
        batchDepth++;
        batchesOpened++;
    }

    @Override
    public void endBatch() {
        if (batchDepth == 0) {
            fail("endBatch without beginBatch");
        }
        batchDepth--;
    }

    @Override
    public void commit(String s) {
        requireBatch("commit");
        calls.add("commit:" + s);
        replaceRegion(s);
        compStart = -1;
        compEnd = -1;
    }

    @Override
    public void setComposing(String s) {
        requireBatch("compose");
        if (terminal) {
            fail("setComposing in terminal mode: " + s);
        }
        calls.add("compose:" + s);
        replaceRegion(s);
        if (s.isEmpty()) {
            compStart = -1;
            compEnd = -1;
        } else {
            compStart = selEnd - s.length();
            compEnd = selEnd;
        }
    }

    @Override
    public void finishComposing() {
        requireBatch("finish");
        if (terminal) {
            fail("finishComposing in terminal mode");
        }
        calls.add("finish");
        compStart = -1;
        compEnd = -1;
    }

    @Override
    public void deleteBefore(int count) {
        requireBatch("del");
        if (terminal) {
            fail("deleteBefore in terminal mode: " + count);
        }
        calls.add("del:" + count);
        int end = selStart;
        int start = Math.max(0, end - count);
        text.delete(start, end);
        int removed = end - start;
        selStart -= removed;
        selEnd -= removed;
        if (compStart >= 0) {
            compStart = shiftAfterDelete(compStart, start, end);
            compEnd = shiftAfterDelete(compEnd, start, end);
            if (compStart == compEnd) {
                compStart = -1;
                compEnd = -1;
            }
        }
    }

    @Override
    public boolean cursorRightAfter(String s) {
        cursorChecks++;
        if (selStart != selEnd || selEnd < s.length()) {
            return false;
        }
        return text.substring(selEnd - s.length(), selEnd).equals(s);
    }

    /** Значения, которые настоящий редактор сообщил бы в onUpdateSelection: selStart, selEnd, compStart, compEnd. */
    public int[] report() {
        return new int[] {selStart, selEnd, compStart, compEnd};
    }

    /** Приложение (или сам IME мимо сеанса) вставляет текст в позицию курсора; составление при этом кончается. */
    public void commitExternally(String s) {
        text.replace(selStart, selEnd, s);
        selStart = selStart + s.length();
        selEnd = selStart;
        compStart = -1;
        compEnd = -1;
    }

    /** Приложение опустошает поле. */
    public void clearExternally() {
        text.setLength(0);
        selStart = 0;
        selEnd = 0;
        compStart = -1;
        compEnd = -1;
    }

    /** Пользователь ставит курсор в позицию; составляемая область остаётся как была. */
    public void moveCursorExternally(int position) {
        selStart = position;
        selEnd = position;
    }

    /** Пользователь выделяет текст. */
    public void selectExternally(int start, int end) {
        selStart = start;
        selEnd = end;
    }

    /** Заменяет составляемую область (иначе выделение, иначе точку курсора) текстом; курсор после него. */
    private void replaceRegion(String s) {
        int start = compStart >= 0 ? compStart : selStart;
        int end = compStart >= 0 ? compEnd : selEnd;
        text.replace(start, end, s);
        selStart = start + s.length();
        selEnd = selStart;
    }

    private static int shiftAfterDelete(int pos, int start, int end) {
        if (pos <= start) {
            return pos;
        }
        return pos >= end ? pos - (end - start) : start;
    }

    private void requireBatch(String what) {
        if (batchDepth == 0) {
            fail("editor write outside a batch: " + what);
        }
    }
}
