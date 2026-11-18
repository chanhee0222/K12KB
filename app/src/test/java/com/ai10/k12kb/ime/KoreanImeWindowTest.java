package com.ai10.k12kb.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTermux;
import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The IME window under default settings (no swipe panel, no prediction). K12KB then asks for
 * its window only for the panel or the prediction bar and otherwise releases it. The Hangul
 * strip lives in that window, so a terminal on Korean must get the window; everything else
 * must ask for it exactly as before.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanImeWindowTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";

    private ImeHarness h;

    private boolean showRequested() {
        return h.ime().windowRequestsForTest().contains("show");
    }

    @Test
    public void koreanTerminalAsksForTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        FakeTermux t = h.focusTerminal(0);
        assertTrue("a show was requested", showRequested());
        assertTrue("no hide after it", h.windowShown());
        h.type("gks");
        assertEquals("한", h.stripText());
        h.type(" ");
        assertTrue(h.windowShown());
        assertEquals("한 ", t.screen());
    }

    /** Keys can come before the deferred start work; the window is asked for all the same. */
    @Test
    public void koreanTerminalBeforeInputViewAsksForTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        h.focusTerminalBeforeInputView(0);
        assertTrue(h.windowShown());
    }

    @Test
    public void switchingToKoreanInTerminalAsksForTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        FakeTermux t = h.focusTerminal(0);
        assertFalse(showRequested());
        assertFalse(h.windowShown());

        h.languageKey(); // to Korean
        assertTrue(h.windowShown());
        h.type("gks");
        assertEquals("한", h.stripText());

        h.languageKey(); // back to English: the window is released as before
        assertFalse(h.windowShown());
        assertEquals("한", t.screen());
    }

    /** Pins the old behaviour: English in a terminal never asks for the window. */
    @Test
    public void englishTerminalNeverAsksForTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        FakeTermux t = h.focusTerminal(0);
        h.type("ls ");
        assertEquals("ls ", t.screen());
        assertFalse(showRequested());
        assertFalse(h.windowShown());
    }

    /** Pins the old behaviour: ordinary fields never ask for the window, on either layout. */
    @Test
    public void ordinaryFieldsNeverAskForTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        FakeTextField english = h.focusText();
        h.type("ab");
        assertEquals("ab|", english.shown());
        h.languageKey();
        FakeTextField korean = h.focusText();
        h.type("gks");
        assertEquals("[한]|", korean.shown());
        assertFalse(showRequested());
        assertFalse(h.windowShown());
    }

    @Test
    public void ordinaryFieldAfterKoreanTerminalReleasesTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        h.focusTerminal(0);
        assertTrue(h.windowShown());
        h.ime().clearWindowRequestsForTest();
        h.focusText();
        assertFalse(showRequested());
        assertFalse(h.windowShown());
    }

    @Test
    public void leavingKoreanTerminalReleasesTheWindow() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        h.ime().onFinishInputView(true);
        h.ime().onFinishInput();
        assertEquals("한", t.screen());
        assertFalse(h.windowShown());
    }
}
