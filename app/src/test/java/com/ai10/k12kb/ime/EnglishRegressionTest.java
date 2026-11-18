package com.ai10.k12kb.ime;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.text.InputType;
import android.view.KeyEvent;

import com.ai10.k12kb.K12KbSettings;
import com.ai10.k12kb.testing.FakeTextField;
import com.ai10.k12kb.testing.ImeHarness;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Pins how English typing on the KEY2 behaves today, before any Korean code exists. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class EnglishRegressionTest {

    private static final String ENGLISH = "bb_key_1_2/english_hw";

    @Test
    public void typesWord() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.type("hello");
        assertEquals("hello|", field.shown());
    }

    @Test
    public void shiftHeldGivesCapital() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.type("Hi");
        assertEquals("Hi|", field.shown());
    }

    /**
     * Long-press gives the Shift variant only with the "long press = Alt symbol"
     * setting off; K12KbSettings turns that setting on by default.
     */
    @Test
    public void longPressCapitalizes() {
        writeSettingBeforeBoot("long_press_alt", false); // K12KbSettings.APP_PREFERENCES_5_LONG_PRESS_ALT
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.longPress(KeyEvent.KEYCODE_A);
        assertEquals("A|", field.shown());
    }

    /** With bundled defaults long-press gives the Alt symbol: Alt+A is '*' in bb_key_1_2/alt_hw.json. */
    @Test
    public void longPressGivesAltSymbolByDefault() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.longPress(KeyEvent.KEYCODE_A);
        assertEquals("*|", field.shown());
    }

    @Test
    public void backspaceDeletes() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.type("ab");
        h.tap(KeyEvent.KEYCODE_DEL);
        assertEquals("a|", field.shown());
    }

    @Test
    public void spaceAfterWord() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText();
        h.type("ab cd");
        assertEquals("ab cd|", field.shown());
    }

    /**
     * auto_capitalization defaults to true (K12KbSettings). It works through the
     * field's caps mode, so it shows only in a field that asks for sentence caps:
     * the first letter typed into such an empty field is upper case.
     */
    @Test
    public void autoCapAtStartIsCurrentBehaviour() {
        ImeHarness h = ImeHarness.key2(ENGLISH);
        FakeTextField field = h.focusText(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        h.type("a");
        assertEquals("A|", field.shown());
    }

    /** ImeHarness.key2 writes only the settings it owns, so this one survives into the boot. */
    private static void writeSettingBeforeBoot(String name, boolean value) {
        RuntimeEnvironment.getApplication()
                .getSharedPreferences(K12KbSettings.APP_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(name, value).commit();
    }
}
