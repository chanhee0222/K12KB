package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_4;
import static android.view.KeyEvent.KEYCODE_ALT_LEFT;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_Q;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_TAB;
import static android.view.KeyEvent.KEYCODE_V;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTermux;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The Hangul behaviour cases of KoreanTypingTest and KoreanForeignActionsTest, run against the
 * Termux-faithful terminal. Whatever ends the syllable sends it with commitText first; nothing
 * is ever taken back, so every case also checks that no DEL reached the terminal.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanTerminalActionsTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";

    /** Alt layer of Q on the KEY2: bb_key_1_2/alt_hw.json, "single-press-alt-mode" of KEYCODE_Q. */
    private static final String ALT_Q = "#";
    /** Alt layer of R on the KEY2 (alt_hw.json); a digit, sent as a KEYCODE_3 key event. */
    private static final String ALT_R = "3";

    private ImeHarness h;
    private FakeTermux t;

    @Before
    public void setUp() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        t = h.focusTerminal(0);
        assertTrue("Korean layout must be active", h.ime().IsHangulActive());
    }

    @After
    public void nothingTakenBack() {
        assertEquals("DELs sent to the terminal", 0, t.dels());
    }

    // region Typing

    @Test
    public void shiftGivesTenseWithoutBreakingSyllable() {
        h.type("dlTek");
        assertEquals("", t.screen());
        assertEquals("있다", h.stripText());
        h.type(" ");
        assertEquals("있다 ", t.screen());
    }

    /**
     * The first 각 was closed by the fourth jamo, which is out of undo reach (the last two jamo)
     * after the sixth, so it has already gone to the terminal; the second waits on the strip.
     */
    @Test
    public void doubleLetterStaysTwo() {
        h.type("rkrrkr");
        assertEquals("각", t.screen());
        assertEquals("각", h.stripText());
        h.type(" ");
        assertEquals("각각 ", t.screen());
    }

    @Test
    public void repeatedKeyFastStaysTwo() {
        h.doubleTap(KEYCODE_R);
        assertEquals("ㄱㄱ", h.stripText());
        h.type(" ");
        assertEquals("ㄱㄱ ", t.screen());
    }

    /** A digit (Alt+R) goes out as a key event; the syllable is committed before it. */
    @Test
    public void digitFinishes() {
        h.type("gks");
        h.altTap(KEYCODE_R);
        assertEquals("한" + ALT_R, t.screen());
        assertEquals("", h.stripText());
    }

    /** Key 4 sits in the letter group but types '$', which is not a jamo. */
    @Test
    public void punctuationFinishes() {
        h.type("gks");
        h.tap(KEYCODE_4);
        assertEquals("한$", t.screen());
        assertEquals("", h.stripText());
    }

    // endregion

    // region Edits that do not come from the session

    @Test
    public void enterMidSyllable() {
        h.type("gks");
        h.tap(KEYCODE_ENTER);
        assertEquals("한\r", t.screen());
        assertEquals("", h.stripText());
    }

    @Test
    public void altSymbolMidSyllable() {
        h.type("gks");
        h.altTap(KEYCODE_Q);
        assertEquals("한" + ALT_Q, t.screen());
    }

    @Test
    public void dpadMidSyllable() {
        h.type("gks");
        h.tap(KEYCODE_DPAD_LEFT);
        assertEquals("한\033[D", t.screen());
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void tabMidSyllable() {
        h.type("gks");
        h.tap(KEYCODE_TAB);
        assertEquals("한\t", t.screen());
        assertFalse(h.ime().hangulComposingForTest());
    }

    /** Ctrl+V reaches Termux as a key (^V, U+0016), after the syllable. */
    @Test
    public void ctrlVMidSyllable() {
        h.type("gks");
        h.clipboard("xy");
        h.ctrl(KEYCODE_V);
        assertEquals("한\u0016", t.screen());
    }

    /** K12KB's own paste (Ctrl+Alt) types the clipboard letter by letter, after the syllable. */
    @Test
    public void pasteByLettersMidSyllable() {
        h.type("gks");
        h.clipboard("xy");
        h.ctrl(KEYCODE_ALT_LEFT);
        assertEquals("한xy", t.screen());
    }

    /**
     * Delete-word (key 0 held + Del) in a terminal: the syllable goes out first. The terminal
     * gives back no text, so delete-word finds no word and deletes nothing.
     */
    @Test
    public void deletePrevWordMidSyllable() {
        h.type("gks rk");
        h.key0(KEYCODE_DEL);
        assertEquals("한 가", t.screen());
        assertFalse(h.ime().hangulComposingForTest());
    }

    // endregion
}
