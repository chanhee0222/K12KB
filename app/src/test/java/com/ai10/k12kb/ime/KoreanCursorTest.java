package com.ai10.k12kb.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Cursor races between the composing syllable and the app's selection reports. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanCursorTest {

    private ImeHarness h;
    private FakeTextField f;

    @Before
    public void setUp() {
        h = ImeHarness.key2("bb_key_1_2/english_hw", "bb_key_1_2/korean_hw");
        h.languageKey();
        f = h.focusText();
        assertTrue("Korean layout must be active", h.ime().IsHangulActive());
    }

    @Test
    public void spaceThenSyllableWithLateReports() {
        f.deferReports(true);
        h.type("gks rk");
        f.deliverReports();
        h.type("s");
        assertEquals("한 [간]|", f.shown());
    }

    @Test
    public void tapElsewhereFinishes() {
        h.type("gks");
        f.setSelectionByUser(0);
        f.deliverReports(); // the report reaches the IME before the next key
        h.type("k");
        assertEquals("ㅏ|한", f.shown().replace("[", "").replace("]", ""));
    }

    // Key-before-report variants: the next key wins the race against the app's report.

    @Test
    public void tapElsewhereFinishesWhenKeyBeatsReport() {
        f.deferReports(true);
        h.type("gks");
        f.setSelectionByUser(0);
        h.type("k");
        assertEquals("ㅏ|한", f.shown().replace("[", "").replace("]", ""));
    }

    @Test
    public void sendButtonClearsFieldWhenKeyBeatsReport() {
        f.deferReports(true);
        h.type("gk");
        f.setText("", 0);
        h.type("s");
        assertEquals("[ㄴ]|", f.shown());
    }

    @Test
    public void appReplacesTextWhenKeyBeatsReport() {
        f.deferReports(true);
        h.type("gk");
        f.setText("abc", 3);
        h.type("s");
        assertEquals("abc[ㄴ]|", f.shown());
    }

    @Test
    public void sendButtonClearsFieldMidSyllable() {
        h.type("gk");
        f.setText("", 0);
        f.deliverReports(); // the report reaches the IME before the next key
        h.type("s");
        assertEquals("[ㄴ]|", f.shown());
    }

    @Test
    public void appReplacesTextMidSyllable() {
        h.type("gk");
        f.setText("abc", 3);
        f.deliverReports(); // the report reaches the IME before the next key
        h.type("s");
        assertEquals("abc[ㄴ]|", f.shown());
    }
}
