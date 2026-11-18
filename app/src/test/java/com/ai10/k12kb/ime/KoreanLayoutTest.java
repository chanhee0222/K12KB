package com.ai10.k12kb.ime;

import static android.view.KeyEvent.*;
import static org.junit.Assert.*;

import com.ai10.k12kb.TestK12KbIME;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class KoreanLayoutTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";
    private static final String KOREAN = "bb_key_1_2/korean_hw";

    private static void assertKeys(TestK12KbIME ime, String plain, String shifted, int... keys) {
        for (int i = 0; i < keys.length; i++) {
            assertEquals("plain " + keys[i], plain.charAt(i), ime.keyToCharCodeForTest(keys[i], false, false));
        }
        for (int i = 0; i < keys.length; i++) {
            assertEquals("shift " + keys[i], shifted.charAt(i), ime.keyToCharCodeForTest(keys[i], false, true));
        }
    }

    @Test
    public void koreanLayoutLoadsAndMapsKeys() {
        ImeHarness h = ImeHarness.key2(ENGLISH, KOREAN);
        h.ime().ChangeLanguageForTest();
        assertEquals(KOREAN, h.ime().currentKeyboardMapping());
        assertTrue(h.ime().IsHangulActive());
        assertEquals('ㅂ', h.ime().keyToCharCodeForTest(KEYCODE_Q, false, false));
        assertEquals('ㅃ', h.ime().keyToCharCodeForTest(KEYCODE_Q, false, true));
        assertEquals('ㅁ', h.ime().keyToCharCodeForTest(KEYCODE_A, false, true));
        assertEquals('ㅡ', h.ime().keyToCharCodeForTest(KEYCODE_M, false, false));
    }

    @Test
    public void fullStandardTwoBeolsikMap() {
        ImeHarness h = ImeHarness.key2(ENGLISH, KOREAN);
        h.ime().ChangeLanguageForTest();
        assertKeys(h.ime(), "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ",
                KEYCODE_Q, KEYCODE_W, KEYCODE_E, KEYCODE_R, KEYCODE_T,
                KEYCODE_Y, KEYCODE_U, KEYCODE_I, KEYCODE_O, KEYCODE_P);
        assertKeys(h.ime(), "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ",
                KEYCODE_A, KEYCODE_S, KEYCODE_D, KEYCODE_F, KEYCODE_G,
                KEYCODE_H, KEYCODE_J, KEYCODE_K, KEYCODE_L);
        assertKeys(h.ime(), "ㅋㅌㅊㅍㅠㅜㅡ", "ㅋㅌㅊㅍㅠㅜㅡ",
                KEYCODE_Z, KEYCODE_X, KEYCODE_C, KEYCODE_V, KEYCODE_B, KEYCODE_N, KEYCODE_M);
    }

    @Test
    public void nonLetterKeysAndAltLayerAreAsInEnglish() {
        ImeHarness h = ImeHarness.key2(ENGLISH, KOREAN);
        int altEnglish = h.ime().keyToCharCodeForTest(KEYCODE_A, true, false);
        assertEquals('$', h.ime().keyToCharCodeForTest(KEYCODE_4, false, false));
        h.ime().ChangeLanguageForTest();
        assertEquals(altEnglish, h.ime().keyToCharCodeForTest(KEYCODE_A, true, false));
        assertEquals('$', h.ime().keyToCharCodeForTest(KEYCODE_4, false, false));
        assertEquals('0', h.ime().keyToCharCodeForTest(KEYCODE_0, false, true));
    }

    @Test
    public void englishIsNotHangul() {
        assertFalse(ImeHarness.key2(ENGLISH).ime().IsHangulActive());
    }

    @Test
    public void koreanIsOffByDefault() {
        assertFalse(ImeHarness.key2(ENGLISH).ime().layoutMappingsForTest().contains(KOREAN));
    }
}
