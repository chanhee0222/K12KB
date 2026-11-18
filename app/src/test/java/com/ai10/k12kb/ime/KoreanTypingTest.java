package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_SHIFT_LEFT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** Korean (2-beolsik) typed through the real key path into an ordinary text field. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanTypingTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";

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
    public void typesKoreanWords() {
        h.type("gksrnrdj");
        assertEquals("한국[어]|", f.shown());
    }

    @Test
    public void shiftGivesTenseWithoutBreakingSyllable() {
        h.type("dlTek");
        assertEquals("있[다]|", f.shown());
    }

    @Test
    public void doubleLetterStaysTwo() {
        h.type("rkrrkr");
        assertEquals("각[각]|", f.shown());
    }

    @Test
    public void repeatedKeyFastStaysTwo() {
        h.doubleTap(KEYCODE_R);
        assertEquals("ㄱ[ㄱ]|", f.shown());
    }

    @Test
    public void spaceFinishes() {
        h.type("gks rk");
        assertEquals("한 [가]|", f.shown());
    }

    @Test
    public void backspaceWalksJamoThenDeletesText() {
        h.type("ekfr");
        h.tap(KEYCODE_DEL);
        assertEquals("[달]|", f.shown());
        h.type(" ");
        h.tap(KEYCODE_DEL);
        h.tap(KEYCODE_DEL);
        assertEquals("|", f.shown());
    }

    /** Fresh field at sentence start: auto-capitalisation must not turn ㅂ into ㅃ. */
    @Test
    public void noAutoCapitalisationTensing() {
        h.type("qkq");
        assertEquals("[밥]|", f.shown());
    }

    /**
     * A plain text field never auto-capitalises (caps mode comes from the field), so the test above
     * cannot catch tensing on its own. This field asks for sentence caps, as English pins in
     * EnglishRegressionTest.autoCapAtStartIsCurrentBehaviour.
     */
    @Test
    public void noAutoCapitalisationTensingInSentenceCapsField() {
        f = h.focusText(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        h.type("qkq");
        assertEquals("[밥]|", f.shown());
    }

    /**
     * The field starts on English, whose start-of-input auto-cap sets first-letter shift; switching to
     * Korean must drop it. Reuses this test's IME: back to English first, then focus the field.
     */
    @Test
    public void englishAutoCapDoesNotLeakIntoKorean() {
        h.languageKey();
        f = h.focusText(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        h.languageKey();
        assertTrue(h.ime().IsHangulActive());
        h.type("qkq");
        assertEquals("[밥]|", f.shown());
    }

    /**
     * A manual one-shot Shift (Left Shift short press: ActionChangeFirstLetterShiftMode) must survive
     * cursor reports that arrive after it. On a device reports are asynchronous, so the reports for
     * ㅇ and ㅣ can land after the Shift tap; here they are held back and delivered after it.
     */
    @Test
    public void manualShiftSurvivesCursorReports() {
        f = h.focusText(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        f.deferReports(true);
        h.type("dl");
        h.tap(KEYCODE_SHIFT_LEFT);
        f.deliverReports();
        f.deferReports(false);
        h.type("t");
        h.type("ek");
        assertEquals("있[다]|", f.shown());
    }

    @Test
    public void switchLanguageMidSyllable() {
        h.type("gks");
        h.languageKey();
        h.type("a");
        h.languageKey();
        h.type("k");
        assertEquals("한a[ㅏ]|", f.shown());
    }

    @Test
    public void languageKeyFinishesSyllable() {
        h.type("gks");
        h.languageKey();
        assertEquals("한|", f.shown());
    }

    @Test
    public void leavingFieldFinishes() {
        h.type("gks");
        h.ime().onFinishInput();
        assertEquals("한|", f.shown());
        // The base class finishes composing text on its own; the session must know it too.
        assertFalse(h.ime().hangulComposingForTest());
    }

    @Test
    public void hidingWindowFinishes() {
        h.type("gks");
        h.ime().onWindowHidden();
        assertEquals("한|", f.shown());
    }

    /** A new field must not receive the old field's syllable, even if onFinishInput was skipped. */
    @Test
    public void newFieldStartsWithoutOldSyllable() {
        h.type("gks");
        FakeTextField second = new FakeTextField(h.ime()::onUpdateSelection);
        EditorInfo info = new EditorInfo();
        info.inputType = InputType.TYPE_CLASS_TEXT;
        info.packageName = "com.example.editor";
        info.fieldId = 2;
        h.ime().attachField(second, info);
        h.ime().onStartInput(info, false);
        ShadowLooper.idleMainLooper();
        h.type("k");
        assertEquals("[ㅏ]|", second.shown());
    }

    /** The user moves the cursor away: the cursor report ends the syllable where it stands. */
    @Test
    public void cursorMoveFinishesSyllable() {
        h.type("gks");
        h.tap(KEYCODE_DPAD_LEFT);
        h.type("k");
        assertEquals("[ㅏ]|한", f.shown());
    }

    /** Back on English. */
    @Test
    public void englishUnaffected() {
        h.languageKey();
        h.type("hello");
        assertEquals("hello|", f.shown());
    }

    /**
     * The session took a Del key-down and its key-up was lost (focus moved, layout switched while
     * Del was held). The next English Del must reach the app whole, key-up included.
     */
    @Test
    public void lostDelKeyUpDoesNotSwallowEnglishDelKeyUp() {
        h.type("gk");
        assertTrue("the session takes Del", h.down(KEYCODE_DEL));
        assertEquals("[ㅎ]|", f.shown());
        h.languageKey(); // English; the Hangul Del is never released
        assertFalse(h.ime().IsHangulActive());
        assertFalse(h.down(KEYCODE_DEL));
        assertEquals("|", f.shown());
        assertFalse("the app gets its key-up", h.up(KEYCODE_DEL));
    }

    /** A language pack installed or removed re-reads the layouts: the syllable is finished first. */
    @Test
    public void languagePackReloadFinishesSyllable() {
        h.type("gks");
        h.ime().reloadLanguagePacksForTest();
        assertEquals("한|", f.shown());
        assertFalse(h.ime().hangulComposingForTest());
        assertTrue(h.ime().IsHangulActive());
    }

    /** A new field starts with no Del held by the session. */
    @Test
    public void newFieldForgetsDelTakenBySession() {
        h.type("gk");
        assertTrue(h.down(KEYCODE_DEL));
        h.focusText();
        assertFalse(h.up(KEYCODE_DEL));
    }
}
