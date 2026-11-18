package com.ai10.k12kb.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class ImeBootTest {

    @Test
    public void imeStartsOnKey2WithEnglish() {
        ImeHarness h = ImeHarness.key2("bb_key_1_2/english_hw");
        assertTrue("onCreate swallowed an exception: " + h.ime().onCreateErrorForTest(), h.started());
        assertEquals("bb_key_1_2/english_hw", h.ime().currentKeyboardMapping());
    }

    @Test
    public void focusMovesToTheNewField() {
        ImeHarness h = ImeHarness.key2("bb_key_1_2/english_hw");
        FakeTextField first = h.focusText();
        h.type("ab");
        FakeTextField second = h.focusText();
        h.type("cd");
        assertEquals("ab|", first.shown());
        assertEquals("cd|", second.shown());
    }
}
