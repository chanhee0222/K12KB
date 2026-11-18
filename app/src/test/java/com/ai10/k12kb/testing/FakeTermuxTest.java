package com.ai10.k12kb.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.view.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class FakeTermuxTest {

    private final FakeTermux t = new FakeTermux();

    @Test
    public void commitGoesStraightToTheScreen() {
        t.commitText("한", 1);
        assertEquals("한", t.screen());
    }

    @Test
    public void composingTextIsHeldUntilFinished() {
        t.setComposingText("하", 1);
        assertEquals("", t.screen());
        t.setComposingText("한", 1);
        t.finishComposingText();
        assertEquals("한", t.screen());
    }

    @Test
    public void commitReplacesHeldComposingText() {
        t.setComposingText("하", 1);
        t.commitText("x", 1);
        t.finishComposingText();
        assertEquals("x", t.screen());
    }

    @Test
    public void deleteSurroundingTextSendsOneDelPerChar() {
        t.commitText("abc", 1);
        t.deleteSurroundingText(2, 0);
        assertEquals("a", t.screen());
        assertEquals(2, t.dels());
    }

    @Test
    public void delKeyErasesAndCounts() {
        t.commitText("ab", 1);
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        assertEquals("a", t.screen());
        assertEquals(1, t.dels());
    }

    /** InputMethodService.sendKeyChar sends digits as key events; Termux types them. */
    @Test
    public void printableKeyTypesItsCharacter() {
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_3));
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_3));
        assertEquals("3", t.screen());
    }

    @Test
    public void ctrlLetterSendsControlCode() {
        int ctrl = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
        t.sendKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, ctrl));
        assertEquals("\u0003", t.screen());
    }

    @Test
    public void modifierKeySendsNothing() {
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
        t.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
        assertEquals("", t.screen());
        assertEquals(0, t.dels());
    }

    @Test
    public void nothingCanBeReadBack() {
        t.commitText("한", 1);
        assertEquals("", t.getTextBeforeCursor(10, 0).toString());
        assertNull(t.getSelectedText(0));
    }
}
