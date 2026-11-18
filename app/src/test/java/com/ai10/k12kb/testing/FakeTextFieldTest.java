package com.ai10.k12kb.testing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.view.KeyEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class FakeTextFieldTest {

    private final List<int[]> reports = new ArrayList<>();
    private FakeTextField f;

    @Before
    public void setUp() {
        f = new FakeTextField((os, oe, ns, ne, cs, ce) -> reports.add(new int[]{os, oe, ns, ne, cs, ce}));
    }

    @Test
    public void shownMarksCursorAndComposingSpan() {
        f.commitText("하", 1);
        f.setComposingText("나", 1);
        assertEquals("하[나]|", f.shown());
        f.finishComposingText();
        assertEquals("하나|", f.shown());
    }

    @Test
    public void commitReplacesComposingSpan() {
        f.setComposingText("ab", 1);
        f.commitText("X", 1);
        assertEquals("X|", f.shown());
    }

    @Test
    public void deleteSurroundingTextCountsFromComposingSpan() {
        f.setText("abc", 3);
        f.setComposingRegion(1, 3);
        f.deleteSurroundingText(1, 0);
        assertEquals("[bc]|", f.shown());
    }

    @Test
    public void reportsArriveThroughMainLooperNotInsideTheCall() {
        f.commitText("a", 1);
        assertEquals(0, reports.size());
        ShadowLooper.idleMainLooper();
        assertEquals(1, reports.size());
        assertArrayEquals(new int[]{0, 0, 1, 1, -1, -1}, reports.get(0));
    }

    @Test
    public void batchGivesOneReportAndUnchangedStateGivesNone() {
        f.beginBatchEdit();
        f.commitText("a", 1);
        f.commitText("b", 1);
        f.endBatchEdit();
        f.finishComposingText();
        ShadowLooper.idleMainLooper();
        assertEquals(1, reports.size());
        assertArrayEquals(new int[]{0, 0, 2, 2, -1, -1}, reports.get(0));
    }

    @Test
    public void deferredReportsWaitForDeliver() {
        f.deferReports(true);
        f.setComposingText("a", 1);
        f.commitText("a", 1);
        ShadowLooper.idleMainLooper();
        assertEquals(0, reports.size());
        f.deliverReports();
        assertEquals(2, reports.size());
        assertArrayEquals(new int[]{0, 0, 1, 1, 0, 1}, reports.get(0));
        assertArrayEquals(new int[]{1, 1, 1, 1, -1, -1}, reports.get(1));
    }

    @Test
    public void appliesKeysTheImeDeclined() {
        f.setText("ab", 2);
        f.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        f.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X));
        assertEquals("ax|", f.shown());
        f.setComposingRegion(0, 2);
        f.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals("a|x", f.shown());
        f.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        assertEquals("a\n|x", f.shown());
    }
}
