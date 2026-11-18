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

    /** Keyboard-mapping resource of the active layout, e.g. "bb_key_1_2/english_hw". */
    public String currentKeyboardMapping() {
        return keyboardLayoutManager.GetCurrentKeyboardLayout().Resources.KeyboardMapping;
    }
}
