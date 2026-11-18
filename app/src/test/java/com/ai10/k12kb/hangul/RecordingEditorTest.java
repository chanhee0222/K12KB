package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Запись выделения в shown() совпадает с FakeTextField: |abc|. */
public final class RecordingEditorTest {
    @Test public void selectionIsBarredOnBothEnds() {
        RecordingEditor ed = new RecordingEditor();
        ed.beginBatch(); ed.commit("abcd"); ed.endBatch();
        ed.selectExternally(1, 3);
        assertEquals("a|bc|d", ed.shown());
    }

    @Test public void composingAndCursor() {
        RecordingEditor ed = new RecordingEditor();
        ed.beginBatch(); ed.commit("하"); ed.setComposing("ㄴ"); ed.endBatch();
        assertEquals("하[ㄴ]|", ed.shown());
    }
}
