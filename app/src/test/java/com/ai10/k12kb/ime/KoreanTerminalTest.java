package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_C;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_Q;
import static android.view.KeyEvent.KEYCODE_R;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

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
    /** Alt+Shift layer of Q on the KEY2: alt_hw.json, "single-press-alt-shift-mode" of KEYCODE_Q. */
    private static final String ALT_SHIFT_Q = "~";

    private ImeHarness h;

    private TextView strip() {
        return h.ime().hangulStripForTest();
    }

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
        assertEquals("no field: strip hidden", View.GONE, strip().getVisibility());
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
        assertEquals(View.GONE, strip().getVisibility());
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
        assertEquals(View.GONE, strip().getVisibility());
    }

    /**
     * The strip keeps its height for the whole terminal session on Korean: showing and hiding
     * it per word would resize the IME window, and Termux would reflow (SIGWINCH) every word.
     */
    @Test
    public void stripStaysVisibleBetweenWordsInTerminal() {
        start();
        FakeTermux t = h.focusTerminal(0);
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("", strip().getText().toString());
        h.type("gks");
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("한", strip().getText().toString());
        h.type(" ");
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("", strip().getText().toString());
        h.type("rk");
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("가", strip().getText().toString());
        h.type(" ");
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("", strip().getText().toString());
        assertEquals("한 가 ", t.screen());
    }

    @Test
    public void languageSwitchInTerminalHidesAndRestoresStrip() {
        start();
        FakeTermux t = h.focusTerminal(0);
        assertEquals(View.VISIBLE, strip().getVisibility());
        h.type("gks");
        h.languageKey(); // to English: the syllable goes to the terminal first
        assertFalse(h.ime().IsHangulActive());
        assertEquals("한", t.screen());
        assertEquals(View.GONE, strip().getVisibility());
        h.languageKey(); // back to Korean
        assertTrue(h.ime().IsHangulActive());
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("", strip().getText().toString());
    }

    /** A terminal focused while English is active: no strip until Korean is chosen. */
    @Test
    public void terminalOnEnglishHasNoStrip() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.focusTerminal(0);
        assertFalse(h.ime().IsHangulActive());
        assertEquals(View.GONE, strip().getVisibility());
        h.languageKey();
        assertEquals(View.VISIBLE, strip().getVisibility());
    }

    /**
     * Short-then-long on Q in a terminal, defaults: both ㅂ are undone on the strip (see
     * KoreanLongPressTest.shortThenLongPressUndoesBothJamo), nothing is taken back with DEL.
     */
    @Test
    public void shortThenLongPressInTerminalNeverSendsDel() {
        start();
        FakeTermux t = h.focusTerminal(0);
        h.type("gk");
        h.tapThenLongPress(KEYCODE_Q);
        h.type(" ");
        assertEquals("하" + ALT_SHIFT_Q + " ", t.screen());
        assertEquals(0, t.dels());
    }

    /** Keys can arrive before the framework has built the input view; the strip catches up. */
    @Test
    public void stripTextSetBeforeInputViewAppearsWhenViewIsBuilt() {
        start();
        FakeTermux t = h.focusTerminalBeforeInputView(0);
        h.type("gks");
        h.createInputView();
        assertEquals(View.VISIBLE, strip().getVisibility());
        assertEquals("한", h.stripText());
        assertEquals("", t.screen());
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
        assertFalse(strip().isClickable());
    }
}
