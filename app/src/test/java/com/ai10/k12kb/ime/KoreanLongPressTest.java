package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_4;
import static android.view.KeyEvent.KEYCODE_Q;
import static android.view.KeyEvent.KEYCODE_S;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Holding a letter key on the Korean layout. The key-down types its jamo at once; the
 * long-press then undoes that jamo (not a backspace: a reshaped syllable comes back whole)
 * and types the replacement.
 *
 * long_press_alt (K12KbSettings.APP_PREFERENCES_5_LONG_PRESS_ALT) is ON by default, so by
 * default the replacement is the key's Alt symbol; with it off, the Shift (tense) jamo.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanLongPressTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";
    private static final String LONG_PRESS_ALT = "long_press_alt";

    /** Alt layer of Q on the KEY2: bb_key_1_2/alt_hw.json, "single-press-alt-mode" of KEYCODE_Q. */
    private static final String ALT_Q = "#";
    /** Alt+Shift layer of Q on the KEY2: alt_hw.json, "single-press-alt-shift-mode" of KEYCODE_Q. */
    private static final String ALT_SHIFT_Q = "~";
    /** Alt layer of KEYCODE_4 on the KEY2 (alt_hw.json); the Korean layer of that key is also '$'. */
    private static final String ALT_4 = "$";

    private ImeHarness h;
    private FakeTextField f;

    /** Boots with bundled defaults: long-press gives the Alt symbol. */
    private void startWithDefaults() {
        start();
    }

    /** Boots with long_press_alt off: long-press gives the Shift variant. */
    private void startWithLongPressCapitalize() {
        ImeHarness.prefBeforeBoot(LONG_PRESS_ALT, false);
        start();
    }

    private void start() {
        h = ImeHarness.key2(ENGLISH, KOREAN);
        h.languageKey();
        f = h.focusText();
        assertTrue("Korean layout must be active", h.ime().IsHangulActive());
    }

    @Test
    public void longPressGivesTense() {
        startWithLongPressCapitalize();
        h.longPress(KEYCODE_Q);
        assertEquals("[ㅃ]|", f.shown());
    }

    /** 갈 + ㅂ made 갋 on key-down; undo brings back 갈, not 가 (which a backspace would leave). */
    @Test
    public void longPressRestoresReshapedSyllable() {
        startWithLongPressCapitalize();
        h.type("rkf");
        h.longPress(KEYCODE_Q);
        assertEquals("갈[ㅃ]|", f.shown());
    }

    @Test
    public void longPressOnKeyWithoutTenseForm() {
        startWithLongPressCapitalize();
        h.type("gk");
        h.longPress(KEYCODE_S);
        assertEquals("[한]|", f.shown());
    }

    @Test
    public void longPressGivesAltSymbolByDefault() {
        startWithDefaults();
        h.type("gk");
        h.longPress(KEYCODE_Q);
        assertEquals("하" + ALT_Q + "|", f.shown());
    }

    @Test
    public void longPressAltSymbolAfterReshapedSyllable() {
        startWithDefaults();
        h.type("rkf");
        h.longPress(KEYCODE_Q);
        assertEquals("갈" + ALT_Q + "|", f.shown());
    }

    /**
     * Short-then-long on Q, defaults. The tap gives 합; the second down is a double-press, and Q
     * has no double-press form, so it types a second ㅂ: 합ㅂ. The Short2ndLongPress then runs
     * ActionSendCharLongPressAltSymbolNoMeta: Q is unpaired, so DeleteLastSymbol undoes the
     * second ㅂ (합); the char before the cursor (합) is not Alt+Q, so the replacement is Alt+Shift+Q;
     * a second DeleteLastSymbol undoes the tap's ㅂ (하). Both ㅂ are gone.
     */
    @Test
    public void shortThenLongPressUndoesBothJamo() {
        startWithDefaults();
        h.type("gk");
        h.tapThenLongPress(KEYCODE_Q);
        assertEquals("하" + ALT_SHIFT_Q + "|", f.shown());
    }

    /**
     * Short-then-long on Q with long_press_alt off: ActionSendCharLongPressCapitalize undoes once
     * (the second ㅂ) and sends the double-press Shift form, which Q does not have (code 0, which
     * SendLetterOrSymbol ignores). What is left is the tap's ㅂ on 하.
     */
    @Test
    public void shortThenLongPressCapitalizeUndoesTheSecondJamo() {
        startWithLongPressCapitalize();
        h.type("gk");
        h.tapThenLongPress(KEYCODE_Q);
        assertEquals("[합]|", f.shown());
    }

    /**
     * Key 4 types '$' (not a jamo), which finishes the syllable and leaves no undo history,
     * so the long-press deletes that '$' from the field the old way before typing Alt+4.
     */
    @Test
    public void longPressOnSymbolKeyStillDeletesTheSymbol() {
        startWithDefaults();
        h.type("gk");
        h.longPress(KEYCODE_4);
        assertEquals("하" + ALT_4 + "|", f.shown());
    }
}
