package com.ai10.k12kb.ime;

import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_T;
import static android.view.KeyEvent.KEYCODE_W;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakePredictionEngine;
import com.ai10.k12kb.testing.FakeTermux;
import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.util.Collections;

/**
 * Word prediction is on. Suggestions, next-word prediction and translation stay off while the
 * Korean layout is active (spec: non-goals), and come back unchanged on the way back to English.
 * Only the native dictionary engine is stubbed (FakePredictionEngine).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanPredictionTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";
    /** Alt layer of R on the KEY2: bb_key_1_2/alt_hw.json, "single-press-alt-mode" of KEYCODE_R. */
    private static final String ALT_R = "3";

    private ImeHarness h;
    private FakePredictionEngine engine;

    private void boot() {
        h = ImeHarness.key2WithPrediction(ENGLISH, KOREAN);
        engine = h.predictionEngine();
        assertTrue(h.started());
    }

    private String bar() {
        return h.ime().suggestionBarTextForTest();
    }

    private boolean barShown() {
        return h.ime().suggestionBarShownForTest();
    }

    // region English: pins today's behaviour

    @Test
    public void englishSuggestsWhileTypingAndAfterSpace() {
        boot();
        FakeTextField f = h.focusText();
        h.type("hel");
        assertEquals("hel|", f.shown());
        assertEquals("hel+", bar());
        h.type(" ");
        assertEquals("after-hel", bar());
        assertEquals("hel|", h.ime().predictorWordsForTest());
    }

    @Test
    public void englishCtrlTIsTranslationToggle() {
        boot();
        h.focusText();
        int toasts = ShadowToast.shownToastCount();
        h.ctrl(KEYCODE_T);
        assertEquals("Ctrl+T answers with a translation toast", toasts + 1, ShadowToast.shownToastCount());
    }

    // endregion

    @Test
    public void koreanFieldShowsNoSuggestions() {
        boot();
        h.languageKey();
        assertTrue(h.ime().IsHangulActive());
        FakeTextField f = h.focusText();
        assertFalse(barShown());
        h.type("gks rk");
        assertEquals("한 [가]|", f.shown());
        assertFalse(barShown());
    }

    /** Neither jamo nor the space, digits and punctuation around them reach the predictor. */
    @Test
    public void koreanDoesNotFeedThePredictor() {
        boot();
        h.languageKey();
        FakeTextField f = h.focusText();
        h.type("gks ");
        h.altTap(KEYCODE_R);
        h.tap(android.view.KeyEvent.KEYCODE_4);
        assertEquals("한 " + ALT_R + "$|", f.shown());
        assertEquals(Collections.emptyList(), engine.requests);
        assertEquals("|", h.ime().predictorWordsForTest());
        assertFalse(barShown());
    }

    @Test
    public void switchingToKoreanHidesTheBarAndBackRestoresIt() {
        boot();
        FakeTextField f = h.focusText();
        h.type("hel");
        assertEquals("hel+", bar());

        h.languageKey(); // Korean
        assertFalse(barShown());
        int before = engine.requests.size();
        h.type("gks");
        assertEquals("hel[한]|", f.shown());
        assertFalse(barShown());
        assertEquals(before, engine.requests.size());

        h.languageKey(); // English again: the bar is back, for the word at the cursor
        assertEquals("hel한|", f.shown());
        h.tap(android.view.KeyEvent.KEYCODE_DEL);
        h.type("p");
        assertEquals("help|", f.shown());
        assertEquals("help+", bar());
    }

    /** Back on English the bar shows again at once, for the word read back from the field. */
    @Test
    public void barReturnsAtOnceForTheWordAtTheCursor() {
        boot();
        FakeTextField f = h.focusText();
        h.type("hel");
        h.languageKey(); // Korean
        assertFalse(barShown());
        assertEquals("|", h.ime().predictorWordsForTest());
        h.languageKey(); // English
        assertEquals("hel|", f.shown());
        assertTrue(barShown());
        assertEquals("hel+", bar());
        assertEquals("|hel", h.ime().predictorWordsForTest());
    }

    @Test
    public void koreanCtrlTDoesNothingTranslationRelated() {
        boot();
        h.languageKey();
        FakeTextField f = h.focusText();
        h.type("gks");
        int toasts = ShadowToast.shownToastCount();
        h.ctrl(KEYCODE_T);
        assertEquals(toasts, ShadowToast.shownToastCount());
        assertFalse(h.ime().translationEnabledForTest());
        assertFalse(barShown());
        assertEquals("한|", f.shown());
    }

    /** With the bar hidden by default, Ctrl+W opens it on English; on Korean it stays shut. */
    @Test
    public void koreanCtrlWDoesNotOpenTheBar() {
        ImeHarness.prefBeforeBoot("prediction_bar_hidden", true); // APP_PREFERENCES_23_PREDICTION_BAR_HIDDEN
        boot();
        h.languageKey();
        h.focusText();
        h.ctrl(KEYCODE_W);
        assertFalse(barShown());
        assertEquals(Collections.emptyList(), engine.requests);
    }

    @Test
    public void englishCtrlWOpensTheHiddenBar() {
        ImeHarness.prefBeforeBoot("prediction_bar_hidden", true);
        boot();
        FakeTextField f = h.focusText();
        h.type("hel");
        assertEquals("", bar());
        h.ctrl(KEYCODE_W);
        assertEquals("hel|", f.shown());
        assertEquals("hel+", bar());
    }

    /** A dictionary load that started on English and lands on Korean paints nothing. */
    @Test
    public void lateDictionaryLoadOnKoreanPaintsNothing() {
        boot();
        h.languageKey();
        h.focusText();
        h.type("gks");
        engine.requests.clear();
        h.ime().dictionaryLoadedForTest("en");
        ShadowLooper.idleMainLooper();
        assertFalse(barShown());
        assertEquals(Collections.emptyList(), engine.requests);
    }

    @Test
    public void koreanTerminalWithPredictionShowsOnlyTheStrip() {
        boot();
        h.languageKey();
        FakeTermux t = h.focusTerminal(0);
        h.type("gks");
        assertEquals("한", h.stripText());
        assertFalse(barShown());
        h.type(" ");
        assertEquals("한 ", t.screen());
        assertFalse(barShown());
        assertEquals(Collections.emptyList(), engine.requests);
    }
}
