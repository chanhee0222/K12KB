package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ai10.k12kb.testing.FakeTextField;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class InputConnectionHangulEditorTest {

    private final FakeTextField field = new FakeTextField((a, b, c, d, e, g) -> { });
    private final InputConnectionHangulEditor editor = new InputConnectionHangulEditor(() -> field);

    @Test
    public void writesThroughTheConnection() {
        editor.beginBatch();
        editor.commit("하");
        editor.setComposing("나");
        editor.endBatch();
        assertEquals("하[나]|", field.shown());
        editor.finishComposing();
        editor.deleteBefore(1);
        assertEquals("하|", field.shown());
    }

    @Test
    public void noConnectionIsNoOpAndCannotJudge() {
        InputConnectionHangulEditor none = new InputConnectionHangulEditor(() -> null);
        none.beginBatch();
        none.commit("하");
        none.setComposing("나");
        none.finishComposing();
        none.deleteBefore(1);
        none.endBatch();
        assertTrue(none.cursorRightAfter("나"));
    }

    @Test
    public void cursorRightAfterMatchingText() {
        field.setText("가한", 2);
        assertTrue(editor.cursorRightAfter("한"));
        assertFalse(editor.cursorRightAfter("나"));
        field.setText("한가", 1);
        assertTrue(editor.cursorRightAfter("한"));
        field.setText("한", 0);
        assertFalse(editor.cursorRightAfter("한"));
    }

    @Test
    public void selectionMeansNotRightAfter() {
        field.setText("한한", 1);
        field.setSelection(1, 2);
        assertEquals("한|한|", field.shown());
        assertFalse(editor.cursorRightAfter("한"));
    }
}
