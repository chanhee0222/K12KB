package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HangulJamoTest {
    @Test
    public void mapsEveryLayoutJamo() {
        for (char ch : "ㅂㅈㄷㄱㅅㅁㄴㅇㄹㅎㅋㅌㅊㅍㅃㅉㄸㄲㅆ".toCharArray()) {
            assertEquals(SemanticJamo.Role.CONTEXTUAL_CONSONANT, HangulJamo.fromChar(ch).role());
        }
        for (char ch : "ㅛㅕㅑㅐㅔㅗㅓㅏㅣㅠㅜㅡㅒㅖ".toCharArray()) {
            assertEquals(SemanticJamo.Role.VOWEL, HangulJamo.fromChar(ch).role());
        }
    }

    @Test
    public void nonJamoIsNull() {
        assertNull(HangulJamo.fromChar('a'));
        assertNull(HangulJamo.fromChar('$'));
        assertNull(HangulJamo.fromChar('ㄳ'));
        assertNull(HangulJamo.fromChar('한'));
        assertFalse(HangulJamo.isJamo('a'));
        assertTrue(HangulJamo.isJamo('ㄱ'));
    }

    @Test
    public void snapshotRestoreRoundTrips() {
        HangulComposer c = new HangulComposer();
        c.input(HangulJamo.fromChar('ㄷ'));
        c.input(HangulJamo.fromChar('ㅏ'));
        c.input(HangulJamo.fromChar('ㄹ'));
        int[] s = c.snapshot();
        c.input(HangulJamo.fromChar('ㄱ'));
        c.restore(s);
        assertEquals("달", c.preeditText());
        c.restore(new int[] {-1, -1, -1});
        assertFalse(c.isComposing());
    }
}
