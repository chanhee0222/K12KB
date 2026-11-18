package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_CTRL_LEFT;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_Q;
import static android.view.KeyEvent.KEYCODE_TAB;
import static android.view.KeyEvent.KEYCODE_V;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.inputmethod.InputConnection;

import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Edits that do not come from the Hangul session (cursor keys, paste, delete-word,
 * Alt symbols, Enter) finish the syllable in progress before they touch the field.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanForeignActionsTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";

    /** Alt layer of Q on the KEY2: bb_key_1_2/alt_hw.json, "single-press-alt-mode" of KEYCODE_Q. */
    private static final String ALT_Q = "#";

    private ImeHarness h;
    private FakeTextField f;

    @Before
    public void setUp() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        f = h.focusText();
        assertTrue("Korean layout must be active", h.ime().IsHangulActive());
    }

    @Test
    public void dpadMidSyllableFinishesFirst() {
        h.type("gks");
        h.tap(KEYCODE_DPAD_LEFT);
        assertEquals("|한", f.shown());
    }

    /**
     * Tab is not in the KEY2 mechanics, so it goes on to the app. FakeTextField does nothing
     * with it, which leaves no cursor report to finish the syllable afterwards: only the
     * onKeyDown guard can.
     */
    @Test
    public void tabMidSyllableFinishesFirst() {
        h.type("gks");
        h.tap(KEYCODE_TAB);
        assertEquals("한|", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void pasteMidSyllable() {
        h.type("gks");
        h.clipboard("x");
        h.ctrl(KEYCODE_V);
        assertEquals("한x|", f.shown());
    }

    /**
     * KEY2 delete-word chord: key 0 held + Del (ActionDeletePrevWord in keyboard_mechanics.json).
     * Delete-word removes the word together with the whitespace before it, in English too
     * ("abc de" gives "abc"), so "한 가" gives "한".
     */
    @Test
    public void deletePrevWordMidSyllable() {
        h.type("gks rk");
        h.key0(KEYCODE_DEL);
        assertEquals("한|", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void altSymbolMidSyllable() {
        h.type("gks");
        h.altTap(KEYCODE_Q);
        assertEquals("한" + ALT_Q + "|", f.shown());
    }

    @Test
    public void heldAltSymbolMidSyllable() {
        h.type("gks");
        h.alt(KEYCODE_Q);
        assertEquals("한" + ALT_Q + "|", f.shown());
    }

    @Test
    public void enterMidSyllable() {
        h.type("gks");
        h.tap(KEYCODE_ENTER);
        assertEquals("한\n|", f.shown());
    }

    // The field's cursor report also finishes the syllable, but only after the edit has
    // landed. With reports held back, these check that the finish comes first.

    @Test
    public void pasteFinishesBeforeTheEditNotOnTheReport() {
        h.type("gks");
        h.clipboard("x");
        f.deferReports(true);
        h.ctrl(KEYCODE_V);
        assertEquals("한x|", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void enterFinishesBeforeTheKeyNotOnTheReport() {
        h.type("gks");
        f.deferReports(true);
        h.tap(KEYCODE_ENTER);
        assertEquals("한\n|", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void dpadFinishesBeforeTheKeyNotOnTheReport() {
        h.type("gks");
        f.deferReports(true);
        h.tap(KEYCODE_DPAD_LEFT);
        assertEquals("|한", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
    }

    /** The session writes through the raw connection, so its own edits never finish the syllable. */
    @Test
    public void sessionWritesDontFinishThemselves() {
        h.type("gk");
        assertEquals("[하]|", f.shown());
    }

    /** One wrapper per field connection, not one per call; none without a connection. */
    @Test
    public void connectionWrapperIsCachedPerField() {
        InputConnection first = h.ime().getCurrentInputConnection();
        assertSame(first, h.ime().getCurrentInputConnection());

        h.focusText();
        InputConnection second = h.ime().getCurrentInputConnection();
        assertNotSame(first, second);
        assertSame(second, h.ime().getCurrentInputConnection());

        h.ime().attachField(null, null);
        assertNull(h.ime().getCurrentInputConnection());
    }

    /** Ctrl on its own sends Ctrl key events to the field; a modifier must not break the syllable. */
    @Test
    public void ctrlAloneKeepsSyllable() {
        h.type("gk");
        h.tap(KEYCODE_CTRL_LEFT);
        h.type("s");
        assertEquals("[한]|", f.shown());
    }
}
