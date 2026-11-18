package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public final class HangulSessionCursorTest {
    private RecordingEditor ed;
    private HangulSession s;

    @Before
    public void setUp() {
        ed = new RecordingEditor();
        s = new HangulSession(ed, text -> { });
    }

    private void typeJamo(HangulSession session, String jamo) {
        for (char c : jamo.toCharArray()) {
            session.input(c);
        }
    }

    private void feed(int[] report) {
        s.onSelectionUpdate(report[0], report[1], report[2], report[3]);
    }

    // Клавиша раньше отчёта: поле уже изменилось, а IME об этом ещё не сообщили.
    @Test public void clearedExternallyWithoutReportStartsFresh() {
        typeJamo(s, "ㅎㅏ"); ed.clearExternally(); typeJamo(s, "ㄴ");
        assertEquals("[ㄴ]|", ed.shown());
    }

    @Test public void cursorMovedExternallyWithoutReportComposesAtNewSpot() {
        typeJamo(s, "ㅎㅏ"); ed.moveCursorExternally(0); typeJamo(s, "ㄴ");
        assertEquals("[ㄴ]|하", ed.shown());
    }

    @Test public void backspaceAfterExternalClearReturnsFalse() {
        typeJamo(s, "ㅎㅏ"); ed.clearExternally();
        assertFalse(s.backspace());
        assertFalse(s.isComposing());
    }

    // Редактор больше не держит слог: откат дописывает сеанс как есть и отвечает true, чтобы
    // вызывающий (долгое нажатие) не стёр вместо джамо настоящий символ поля.
    @Test public void undoAfterExternalClearFinishesAndDeletesNothing() {
        typeJamo(s, "ㅎㅏ"); ed.clearExternally();
        assertTrue(s.undoLastJamo());
        assertFalse(s.isComposing());
        assertEquals("", ed.text());
        assertFalse(ed.calls.stream().anyMatch(c -> c.startsWith("del:")));
    }

    // Без истории (после финиша) откат по-прежнему отказывается: удаляет вызывающий.
    @Test public void undoAfterExternalClearAndFinishHasNoHistory() {
        typeJamo(s, "ㅎㅏ"); ed.clearExternally();
        s.undoLastJamo();
        assertFalse(s.undoLastJamo());
    }

    @Test public void ownEchoesKeepComposing() { typeJamo(s, "ㅎㅏ"); feed(ed.report()); typeJamo(s, "ㄴ"); assertEquals("[한]|", ed.shown()); }

    // Отчёт, согласный с набором (курсор свёрнут в конце составляемой области), редактор не читает.
    @Test public void reportConsistentWithComposingDoesNotReadEditor() {
        typeJamo(s, "ㅎㅏ");
        int before = ed.cursorChecks;
        feed(ed.report());
        assertEquals(before, ed.cursorChecks);
        assertTrue(s.isComposing());
    }

    // Любой другой отчёт — повод сверить редактор вживую.
    @Test public void otherReportReadsEditor() {
        typeJamo(s, "ㅎㅏ");
        int before = ed.cursorChecks;
        s.onSelectionUpdate(1, 1, -1, -1);
        assertEquals(before + 1, ed.cursorChecks);
    }

    @Test public void lateEchoOfForeignWriteIsIgnored() {
        ed.commitExternally(" ");                    // пробел, записанный IME мимо сеанса
        typeJamo(s, "ㄴ");
        s.onSelectionUpdate(1, 1, -1, -1);           // отчёт о пробеле приходит с опозданием
        typeJamo(s, "ㅏ");
        assertEquals(" [나]|", ed.shown());
    }

    @Test public void staleReportsOfEarlierSyllableAreIgnored() {
        typeJamo(s, "ㅎㅏㄴ"); int[] old = ed.report(); s.finish(); ed.commitExternally(" ");
        typeJamo(s, "ㄱㅏ");
        feed(old); s.onSelectionUpdate(2, 2, -1, -1);   // запоздалые: о старом слоге, потом о пробеле
        typeJamo(s, "ㄴ");
        assertEquals("한 [간]|", ed.shown());
    }

    @Test public void userMovedCursorFinishes() {
        typeJamo(s, "ㅎㅏ"); ed.moveCursorExternally(0);
        s.onSelectionUpdate(0, 0, 0, 1);             // касание перед слогом
        assertFalse(s.isComposing()); assertTrue(ed.calls.contains("finish"));
        assertEquals("하", ed.text());                // слог остаётся в поле как есть
        assertEquals("|하", ed.shown());
    }

    @Test public void staleRangeReportIsIgnored() { typeJamo(s, "ㅎㅏ"); s.onSelectionUpdate(0, 1, -1, -1); assertTrue(s.isComposing()); }

    @Test public void appDroppedCompositionStartsFresh() {
        typeJamo(s, "ㅎㅏ"); feed(ed.report());
        ed.clearExternally();  s.onSelectionUpdate(0, 0, -1, -1);   // «Отправить» в чате опустошило поле
        typeJamo(s, "ㄴ");
        assertEquals("[ㄴ]|", ed.shown());
    }

    @Test public void terminalModeIgnoresReports() { s.setTerminalMode(true); typeJamo(s, "ㅎㅏ"); s.onSelectionUpdate(5, 7, -1, -1); assertTrue(s.isComposing()); }
}
