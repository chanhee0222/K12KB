package com.ai10.k12kb.hangul;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EditorKindTest {
    @Test
    public void termuxDefaultIsTerminal() {
        assertTrue(EditorKind.isTerminal(0, -1));
    }

    /** Termux "enforce char-based input": VISIBLE_PASSWORD | NO_SUGGESTIONS, still no class bits. */
    @Test
    public void termuxCharBasedIsTerminal() {
        assertTrue(EditorKind.isTerminal(0x80090, -1));
    }

    @Test
    public void textFieldIsNot() {
        assertFalse(EditorKind.isTerminal(0x1, 0));
    }

    @Test
    public void nullTypeWithCursorIsNot() {
        assertFalse(EditorKind.isTerminal(0, 3));
    }

    @Test
    public void textClassWithoutCursorIsNot() {
        assertFalse(EditorKind.isTerminal(0x1, -1));
    }
}
