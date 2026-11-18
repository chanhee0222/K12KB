package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_C;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_R;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;

import com.ai10.k12kb.testing.FakeTermux;
import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/**
 * Korean in a terminal (Termux). Nothing sent to a terminal can be taken back, so the
 * syllable in progress, and text an undoable jamo closed, wait on the strip above the
 * keyboard and go to the terminal with commitText only once they are final.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanTerminalTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";
    private static final String LONG_PRESS_ALT = "long_press_alt";

    /** Alt layer of R on the KEY2: bb_key_1_2/alt_hw.json, "single-press-alt-mode" of KEYCODE_R. */
    private static final String ALT_R = "3";

    private ImeHarness h;

    private void start() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        assertTrue("Korean layout must be active", h.ime().IsHangulActive());
    }

    @Test
    public void terminalShowsStripThenSends() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        assertEquals("", t.screen());
        assertEquals("한", h.stripText());
        h.type(" ");
        assertEquals("한 ", t.screen());
        assertEquals("", h.stripText());
    }

    @Test
    public void terminalCharBasedMode() {
        start();
        FakeTermux t = h.focusTerminal(0x80090);
        h.type("gks ");
        assertEquals("한 ", t.screen());
    }

    /** Defaults: long-press gives the Alt symbol. 한 + ㄱ closed 한; undo takes it back off the strip. */
    @Test
    public void longPressInTerminalNeverSendsDel() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.longPress(KEYCODE_R);
        h.type(" ");
        assertEquals("한" + ALT_R + " ", t.screen());
        assertEquals(0, t.dels());
    }

    @Test
    public void longPressCapitalizeInTerminalNeverSendsDel() {
        ImeHarness.prefBeforeBoot(LONG_PRESS_ALT, false);
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.longPress(KEYCODE_R);
        h.type(" ");
        assertEquals("한ㄲ ", t.screen());
        assertEquals(0, t.dels());
    }

    @Test
    public void backspaceInTerminal() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("ekfr");
        h.tap(KEYCODE_DEL);
        assertEquals("달", h.stripText());
        assertEquals(0, t.dels());
    }

    @Test
    public void backspaceInTerminalWhenIdlePassesThrough() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks ");
        h.tap(KEYCODE_DEL);
        assertEquals("한", t.screen());
        assertEquals(1, t.dels());
    }

    @Test
    public void leavingTerminalCommitsSyllable() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.ime().onFinishInput();
        assertEquals("한", t.screen());
        assertEquals("", h.stripText());
    }

    @Test
    public void hidingWindowInTerminalCommitsSyllable() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.ime().onWindowHidden();
        assertEquals("한", t.screen());
        assertEquals("", h.stripText());
    }

    @Test
    public void ctrlCInTerminalSendsSyllableFirst() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.ctrl(KEYCODE_C);
        assertTrue(t.screen().startsWith("한"));
        assertEquals("한\u0003", t.screen());
    }

    @Test
    public void stripStaysHiddenInOrdinaryField() {
        start();
        FakeTextField f = h.focusText();
        h.type("gks");
        assertEquals("[한]|", f.shown());
        assertEquals("", h.stripText());
        assertEquals(View.GONE, h.ime().hangulStripForTest().getVisibility());
    }

    /** Leaving a terminal for an ordinary field: strip hidden, Korean composes in the field again. */
    @Test
    public void ordinaryFieldAfterTerminalComposesInField() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        FakeTextField f = h.focusText();
        assertEquals("한", t.screen());
        assertEquals("", h.stripText());
        h.type("rk");
        assertEquals("[가]|", f.shown());
        assertEquals("", h.stripText());
    }

    /** The session can report a preedit before the framework has built the input view. */
    @Test
    public void stripTextSetBeforeInputViewAppearsWhenViewIsBuilt() {
        start();
        h.ime().showHangulPreeditForTest("한");
        h.createInputView();
        assertEquals("한", h.stripText());
    }

    @Test
    public void stripUpdateFromAnotherThreadWaitsForMainThread() throws InterruptedException {
        start();
        h.focusTerminal(0);
        Thread worker = new Thread(() -> h.ime().showHangulPreeditForTest("한"));
        worker.start();
        worker.join();
        assertEquals("", h.stripText());
        ShadowLooper.idleMainLooper();
        assertEquals("한", h.stripText());
    }

    @Test
    public void stripIsNotTappable() {
        start();
        h.focusTerminal(0);
        h.type("gks");
        assertFalse(h.ime().hangulStripForTest().isClickable());
    }
}
