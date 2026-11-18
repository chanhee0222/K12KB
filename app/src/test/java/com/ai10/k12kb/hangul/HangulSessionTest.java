package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public final class HangulSessionTest {
    private static final class Strip implements HangulSession.PreeditStrip {
        String last;

        @Override
        public void show(String text) {
            last = text;
        }
    }

    private RecordingEditor ed;
    private Strip strip;
    private HangulSession s;

    @Before
    public void setUp() {
        ed = new RecordingEditor();
        strip = new Strip();
        s = new HangulSession(ed, strip);
    }

    private void typeJamo(HangulSession session, String jamo) {
        for (char c : jamo.toCharArray()) {
            session.input(c);
        }
    }

    private void goTerminal() {
        ed.terminal = true;
        s.setTerminalMode(true);
    }

    @Test public void composesWords() {
        typeJamo(s, "ㅎㅏㄴㄱㅜㄱㅇㅓ"); assertEquals("한국[어]|", ed.shown());
        s.finish();                       assertEquals("한국어|", ed.shown());
    }

    @Test public void finalMovesToNextSyllable() { typeJamo(s, "ㄷㅏㄹㄱㅏ"); assertEquals("달[가]|", ed.shown()); }

    @Test public void loneJamo() { typeJamo(s, "ㄱㄴ"); assertEquals("ㄱ[ㄴ]|", ed.shown()); }

    @Test public void backspaceWalksJamo() {
        typeJamo(s, "ㄷㅏㄹㄱ");
        assertTrue(s.backspace()); assertEquals("[달]|", ed.shown());
        assertTrue(s.backspace()); assertEquals("[다]|", ed.shown());
        assertTrue(s.backspace()); assertEquals("[ㄷ]|", ed.shown());
        assertTrue(s.backspace()); assertEquals("|", ed.shown());
        assertFalse(s.backspace());
    }

    @Test public void undoRestoresClosedSyllable() {
        typeJamo(s, "ㅎㅏㄴㄱ"); assertEquals("한[ㄱ]|", ed.shown());
        assertTrue(s.undoLastJamo()); assertEquals("[한]|", ed.shown());
    }

    @Test public void undoTwice() {
        typeJamo(s, "ㄱㅏㄹㅂ"); s.undoLastJamo(); s.undoLastJamo();
        assertEquals("[가]|", ed.shown());
    }

    @Test public void undoDepthIsLimited() {
        typeJamo(s, "ㄱㅏㄹㅂ");
        assertTrue(s.undoLastJamo()); assertTrue(s.undoLastJamo());
        assertFalse(s.undoLastJamo());
    }

    @Test public void undoAfterBackspaceIsRefused() {
        typeJamo(s, "ㄱㅏ"); s.backspace(); assertFalse(s.undoLastJamo());
    }

    @Test public void undoWithoutHistoryIsRefused() { assertFalse(s.undoLastJamo()); }

    @Test public void inputRejectsNonJamo() {
        assertThrows(IllegalArgumentException.class, () -> s.input('a'));
    }

    @Test public void inputIsOneBatchOfCommitThenCompose() {
        typeJamo(s, "ㅎㅏㄴ");
        ed.calls.clear(); int before = ed.batchesOpened;
        s.input('ㄱ');
        assertEquals(Arrays.asList("commit:한", "compose:ㄱ"), ed.calls);
        assertEquals(before + 1, ed.batchesOpened);
        assertFalse(ed.inBatch());
    }

    @Test public void undoWithoutCommitIsComposeOnly() {
        typeJamo(s, "ㄱㅏ");
        ed.calls.clear();
        assertTrue(s.undoLastJamo());
        assertEquals(Arrays.asList("compose:ㄱ"), ed.calls);
        assertEquals("[ㄱ]|", ed.shown());
    }

    @Test public void undoAfterCommitClearsDeletesThenReopens() {
        typeJamo(s, "ㅎㅏㄴㄱ");
        ed.calls.clear();
        s.undoLastJamo();
        assertEquals(Arrays.asList("compose:", "del:1", "compose:한"), ed.calls);
    }

    @Test public void undoFirstJamoLeavesNothing() {
        s.input('ㄱ'); assertTrue(s.undoLastJamo());
        assertEquals("|", ed.shown()); assertFalse(s.isComposing());
    }

    @Test public void backspaceToEmptyFinishesComposing() {
        s.input('ㄱ'); ed.calls.clear();
        assertTrue(s.backspace());
        assertEquals(Arrays.asList("compose:", "finish"), ed.calls);
    }

    @Test public void finishAndResetWhenIdleWriteNothing() {
        s.finish(); s.reset();
        assertTrue(ed.calls.isEmpty());
        assertEquals("", strip.last);
    }

    @Test public void resetWritesNothingAndForgets() {
        typeJamo(s, "ㅎㅏㄴㄱ"); ed.calls.clear();
        s.reset();
        assertTrue(ed.calls.isEmpty());
        assertFalse(s.isComposing()); assertFalse(s.undoLastJamo());
    }

    @Test public void isComposingTracksComposer() {
        assertFalse(s.isComposing()); s.input('ㄱ'); assertTrue(s.isComposing());
        s.finish(); assertFalse(s.isComposing());
    }

    @Test public void undoAfterCommitInComposingMode() {
        typeJamo(s, "ㅎㅏㄴㅏ"); assertEquals("하[나]|", ed.shown());
        assertTrue(s.undoLastJamo()); assertEquals("[한]|", ed.shown());
    }

    @Test public void backspaceAfterCommitInComposingMode() {
        typeJamo(s, "ㅎㅏㄴㅏ");
        assertTrue(s.backspace()); assertEquals("하[ㄴ]|", ed.shown());
    }

    // --- terminal mode ---

    @Test public void terminalUndoAfterSyllableBreakLeavesStripOnPreviousSyllable() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㅏ"); assertEquals("하나", strip.last);
        assertTrue(s.undoLastJamo());
        assertEquals("한", strip.last); assertEquals("", ed.text()); assertTrue(ed.calls.isEmpty());
    }

    @Test public void terminalBackspaceAfterSyllableBreakCommitsEarlierSyllable() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㅏ");
        assertTrue(s.backspace());
        assertEquals("하", ed.text()); assertEquals("ㄴ", strip.last);
    }

    @Test public void terminalShowsSyllableOnStrip() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴ"); assertEquals("", ed.text()); assertEquals("한", strip.last);
    }

    @Test public void terminalHoldsBackUntilOutOfUndoReach() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱㅜ");
        assertEquals("", ed.text()); assertEquals("한구", strip.last);
        typeJamo(s, "ㄱ");
        assertEquals("한", ed.text()); assertEquals("국", strip.last);
    }

    @Test public void terminalUndoNeverDeletes() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱ"); assertTrue(s.undoLastJamo());
        assertEquals("", ed.text()); assertEquals("한", strip.last);
    }

    @Test public void terminalUndoStopsAtSentText() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱㅜㄱ");
        assertTrue(s.undoLastJamo()); assertTrue(s.undoLastJamo());
        assertFalse(s.undoLastJamo());
        assertEquals("한", ed.text());
    }

    @Test public void finishInTerminalCommitsHeldText() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱㅜ"); s.finish();
        assertEquals("한구", ed.text()); assertEquals("", strip.last);
        assertFalse(s.isComposing());
    }

    @Test public void terminalBackspaceSendsHeldThenShrinksPreedit() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱ");
        assertTrue(s.backspace());
        assertEquals("한", ed.text()); assertEquals("", strip.last);
        assertFalse(s.backspace());
    }

    @Test public void terminalBackspaceWalksPreedit() {
        goTerminal();
        typeJamo(s, "ㄷㅏㄹ");
        assertTrue(s.backspace()); assertEquals("다", strip.last);
        assertEquals("", ed.text());
    }

    @Test public void terminalResetDropsHeldWithoutWriting() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱ"); s.reset();
        assertEquals("", ed.text()); assertEquals("", strip.last);
    }

    @Test public void switchingModeFinishesInOldMode() {
        typeJamo(s, "ㅎㅏㄴ");
        s.setTerminalMode(true); ed.terminal = true;
        assertEquals("한|", ed.shown()); assertTrue(s.isTerminalMode());
        assertFalse(s.isComposing());
    }

    @Test public void switchingToComposingFlushesTerminalHeldText() {
        goTerminal();
        typeJamo(s, "ㅎㅏㄴㄱ");
        s.setTerminalMode(false); ed.terminal = false;
        assertEquals("한ㄱ", ed.text()); assertFalse(s.isTerminalMode());
        assertEquals("", strip.last);
    }

    @Test public void cursorRightAfterReadsLiveText() {
        typeJamo(s, "ㅎㅏㄴㄱ");
        assertTrue(ed.cursorRightAfter("한ㄱ")); assertTrue(ed.cursorRightAfter("ㄱ"));
        assertFalse(ed.cursorRightAfter("ㄴ"));
        assertFalse(ed.cursorRightAfter("가한ㄱ"));
    }
}
