package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class FinishingInputConnectionTest {

    private final List<String> log = new ArrayList<>();
    private InputConnection target;
    private FinishingInputConnection ic;

    @Before
    public void setUp() {
        target = (InputConnection) Proxy.newProxyInstance(
                InputConnection.class.getClassLoader(),
                new Class<?>[]{InputConnection.class},
                (proxy, method, args) -> {
                    log.add(method.getName());
                    Class<?> r = method.getReturnType();
                    if (r == boolean.class) return true;
                    if (r == int.class) return 0;
                    return null;
                });
        ic = new FinishingInputConnection(target, () -> log.add("finish"));
    }

    @Test
    public void editsFinishFirst() {
        ic.commitText("a", 1);
        ic.setComposingText("a", 1);
        ic.setComposingRegion(0, 1);
        ic.finishComposingText();
        ic.deleteSurroundingText(1, 0);
        ic.deleteSurroundingTextInCodePoints(1, 0);
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
        ic.setSelection(0, 0);
        ic.performContextMenuAction(android.R.id.paste);
        ic.performEditorAction(0);
        ic.commitCompletion(null);
        ic.commitCorrection(null);
        ic.commitContent(null, 0, null);
        assertEquals(Arrays.asList(
                "finish", "commitText",
                "finish", "setComposingText",
                "finish", "setComposingRegion",
                "finish", "finishComposingText",
                "finish", "deleteSurroundingText",
                "finish", "deleteSurroundingTextInCodePoints",
                "finish", "sendKeyEvent",
                "finish", "setSelection",
                "finish", "performContextMenuAction",
                "finish", "performEditorAction",
                "finish", "commitCompletion",
                "finish", "commitCorrection",
                "finish", "commitContent"), log);
    }

    @Test
    public void readsBatchesAndServiceCallsPassStraightThrough() {
        ic.getTextBeforeCursor(1, 0);
        ic.getTextAfterCursor(1, 0);
        ic.getSelectedText(0);
        ic.getCursorCapsMode(0);
        ic.getExtractedText(null, 0);
        ic.beginBatchEdit();
        ic.endBatchEdit();
        ic.clearMetaKeyStates(0);
        ic.requestCursorUpdates(0);
        assertEquals(Arrays.asList(
                "getTextBeforeCursor", "getTextAfterCursor", "getSelectedText", "getCursorCapsMode",
                "getExtractedText", "beginBatchEdit", "endBatchEdit", "clearMetaKeyStates",
                "requestCursorUpdates"), log);
    }

    @Test
    public void modifierKeyEventsDoNotFinish() {
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT));
        assertEquals(Arrays.asList("sendKeyEvent", "sendKeyEvent", "sendKeyEvent"), log);
    }

    @Test
    public void knowsWhichConnectionItWraps() {
        assertTrue(ic.wraps(target));
        assertFalse(ic.wraps(null));
    }
}
