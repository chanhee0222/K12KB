package com.ai10.k12kb;

import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * K12KbIME with the system side replaced by the test harness: the field and its
 * EditorInfo come from {@code ImeHarness} instead of an InputMethodManager binding.
 *
 * Lives in package com.ai10.k12kb so it can read package-private state
 * ({@code isNotStarted}, {@code onCreateError}, layout options) without making
 * any of it public.
 *
 * Stubs: none. The real onCreate boots under Robolectric as is.
 */
public class TestK12KbIME extends K12KbIME {

    private InputConnection field;
    private EditorInfo editorInfo;

    /**
     * Robolectric keeps static fields from one test to the next but gives every test
     * a new Application. K12KbSettings caches the SharedPreferences of the first one,
     * so without this a test would read the previous test's settings.
     */
    public static void resetStaticStateForTest() {
        K12KbSettings._instance = null;
    }

    /** Binds a field, as InputMethodManager does when an editor gains focus. */
    public void attachField(InputConnection field, EditorInfo editorInfo) {
        this.field = field;
        this.editorInfo = editorInfo;
    }

    @Override
    protected InputConnection RawInputConnection() {
        return field;
    }

    @Override
    public EditorInfo getCurrentInputEditorInfo() {
        return editorInfo;
    }

    /**
     * The framework sets this in doStartInput, which the harness bypasses by calling
     * onStartInput directly. IsInputMode() reads it, so it has to follow the binding.
     */
    @Override
    public boolean getCurrentInputStarted() {
        return field != null;
    }

    public void ChangeLanguageForTest() {
        ChangeLanguage();
    }

    public boolean isStartedForTest() {
        return !isNotStarted;
    }

    /** Stack trace that onCreate swallowed, or null if it booted. */
    public String onCreateErrorForTest() {
        return onCreateError;
    }

    /** Hangul composition is on for the active layout. */
    @Override
    public boolean IsHangulActive() {
        return super.IsHangulActive();
    }

    /** What the active layout sends for a key; builds the (private-constructor) KeyPressData by reflection. */
    public int keyToCharCodeForTest(int keyCode, boolean alt, boolean shift) {
        try {
            java.lang.reflect.Constructor<KeyPressData> c = KeyPressData.class.getDeclaredConstructor();
            c.setAccessible(true);
            KeyPressData data = c.newInstance();
            data.KeyCode = keyCode;
            return keyboardLayoutManager.KeyToCharCode(data, alt, shift, false);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Keyboard-mapping resources of every layout in the cycle list. */
    public java.util.List<String> layoutMappingsForTest() {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (KeyboardLayout layout : keyboardLayoutManager.KeyboardLayoutList)
            result.add(layout.Resources.KeyboardMapping);
        return result;
    }

    /** Keyboard-mapping resource of the active layout, e.g. "bb_key_1_2/english_hw". */
    public String currentKeyboardMapping() {
        return keyboardLayoutManager.GetCurrentKeyboardLayout().Resources.KeyboardMapping;
    }
}
