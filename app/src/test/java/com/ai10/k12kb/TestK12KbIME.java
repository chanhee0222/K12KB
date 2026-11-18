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
    /** Window requests the IME made, in order: "show" or "hide". */
    private final java.util.List<String> windowRequests = new java.util.ArrayList<>();

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

    // region IME window: recorded, then passed on to the framework (a no-op without a window token)

    @Override
    protected void RequestShowSelfImplicit() {
        windowRequests.add("show");
        super.RequestShowSelfImplicit();
    }

    @Override
    public void showWindow(boolean showInput) {
        windowRequests.add("show");
        super.showWindow(showInput);
    }

    @Override
    public void requestHideSelf(int flags) {
        windowRequests.add("hide");
        super.requestHideSelf(flags);
    }

    @Override
    public void hideWindow() {
        windowRequests.add("hide");
        super.hideWindow();
    }

    /** Every show/hide request so far, oldest first. */
    public java.util.List<String> windowRequestsForTest() {
        return new java.util.ArrayList<>(windowRequests);
    }

    public void clearWindowRequestsForTest() {
        windowRequests.clear();
    }

    /**
     * The IME window is on screen: the IME's last request was a show. On the KEY2 nothing else
     * shows it for us: with the hardware keyboard an app need not ask for the soft input at all.
     */
    public boolean windowShownForTest() {
        return !windowRequests.isEmpty() && "show".equals(windowRequests.get(windowRequests.size() - 1));
    }

    // endregion

    // region Prediction (booted with prediction_enabled = true)

    /**
     * Puts engine in place of the native SymSpell engine. WordPredictor keeps its engine in static
     * fields that outlive the IME (and Robolectric keeps statics between tests), so both the static
     * cache and this IME's predictor are pointed at the new engine. Only the engine is stubbed.
     */
    public void installPredictionEngineForTest(com.ai10.k12kb.prediction.PredictionEngine engine) {
        if (wordPredictor == null)
            throw new IllegalStateException("prediction is off: boot with prediction_enabled = true");
        try {
            Class<?> c = com.ai10.k12kb.prediction.WordPredictor.class;
            java.lang.reflect.Field dictSize = c.getDeclaredField("dictSize");
            dictSize.setAccessible(true);
            setStatic(c, "sharedEngine", engine);
            setStatic(c, "sharedEngineMode", com.ai10.k12kb.prediction.WordPredictor.ENGINE_NATIVE_SYMSPELL);
            setStatic(c, "sharedEngineDictSize", dictSize.getInt(wordPredictor));
            java.lang.reflect.Method adopt = c.getDeclaredMethod("adoptEngine",
                    com.ai10.k12kb.prediction.PredictionEngine.class);
            adopt.setAccessible(true);
            adopt.invoke(wordPredictor, engine);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setStatic(Class<?> c, String name, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    /** The suggestion bar is VISIBLE (it may still be empty). */
    public boolean suggestionBarShownForTest() {
        return suggestionBar != null && suggestionBar.getVisibility() == android.view.View.VISIBLE;
    }

    /** Words on the suggestion bar, comma-separated; "" when the bar is hidden or absent. */
    public String suggestionBarTextForTest() {
        if (suggestionBar == null || suggestionBar.getVisibility() != android.view.View.VISIBLE)
            return "";
        StringBuilder sb = new StringBuilder();
        collectText(suggestionBar, sb);
        return sb.toString();
    }

    private static void collectText(android.view.View view, StringBuilder sb) {
        if (view instanceof android.widget.TextView) {
            CharSequence text = ((android.widget.TextView) view).getText();
            if (text != null && text.length() > 0) {
                if (sb.length() > 0) sb.append(',');
                sb.append(text);
            }
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                collectText(group.getChildAt(i), sb);
        }
    }

    /** The predictor's tracked words, "previous|current". */
    public String predictorWordsForTest() {
        return wordPredictor.getPreviousWord() + "|" + wordPredictor.getCurrentWord();
    }

    public boolean translationEnabledForTest() {
        return translationManager != null && translationManager.isEnabled();
    }

    /** As if a dictionary load that started earlier finished now. */
    public void dictionaryLoadedForTest(String locale) {
        onDictionaryLoaded(locale);
    }

    // endregion

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

    /** A Hangul syllable is in progress (or, in a terminal, held back). */
    public boolean hangulComposingForTest() {
        return hangulSession.isComposing();
    }

    /**
     * Text on the Hangul strip as the user sees it: "" when the strip is hidden, the input view
     * was never built, or the IME window that holds the strip is not on screen.
     */
    public String hangulStripTextForTest() {
        if (hangulStrip == null || hangulStrip.getVisibility() != android.view.View.VISIBLE)
            return "";
        if (!windowShownForTest())
            return "";
        return hangulStrip.getText().toString();
    }

    /** The strip view itself (null before the input view is built). */
    public android.widget.TextView hangulStripForTest() {
        return hangulStrip;
    }

    public void showHangulPreeditForTest(String text) {
        ShowHangulPreedit(text);
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
