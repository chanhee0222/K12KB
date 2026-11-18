package com.ai10.k12kb.testing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.SystemClock;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import com.ai10.k12kb.K12KbSettings;
import com.ai10.k12kb.TestK12KbIME;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowBuild;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;

/**
 * Boots the real K12KbIME on a simulated BlackBerry KEY2 and drives it with
 * hardware key events at realistic timing.
 *
 * Timing is checked against assets/keyboard_core.json: time-short-press 175,
 * time-double-press 225, time-triple-press 275, time-long-press 600.
 */
public final class ImeHarness {

    /** Pause after every key gesture; longer than time-triple-press, so presses never combine. */
    public static final long KEY_GAP_MS = 300;
    /** How long a tap holds the key down; shorter than time-short-press. */
    public static final long TAP_HOLD_MS = 50;
    /** Between the two key-downs of doubleTap; shorter than time-double-press. */
    public static final long DOUBLE_TAP_INTERVAL_MS = 100;
    /** Auto-repeat interval while a key is held. */
    public static final long REPEAT_INTERVAL_MS = 50;
    /** How long longPress holds the key; longer than time-long-press. */
    public static final long LONG_PRESS_HOLD_MS = 700;

    /** Pref name K12KB generates for a layout: KeyboardLayoutOptions.getPreferenceName(). */
    private static final String LAYOUT_PREF_PREFIX = "GENERATED_PREF_KEYBOARD_LAYOUT_";
    private static final int META_SHIFT_LEFT = KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
    private static final int META_CTRL_LEFT = KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    private static final int META_ALT_LEFT = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

    private final TestK12KbIME ime;
    private FakeTextField field;

    private ImeHarness(TestK12KbIME ime) {
        this.ime = ime;
    }

    /**
     * A KEY2 with exactly the given layouts enabled, e.g. "bb_key_1_2/english_hw",
     * and prediction off. Runs the IME's onCreate.
     */
    public static ImeHarness key2(String... enabledKeyboardMappings) {
        ShadowBuild.setManufacturer("BlackBerry");
        ShadowBuild.setModel("BBF100-1 KEY2");

        TestK12KbIME.resetStaticStateForTest();
        K12KbSettings settings = K12KbSettings.Get(RuntimeEnvironment.getApplication()
                .getSharedPreferences(K12KbSettings.APP_PREFERENCES, Context.MODE_PRIVATE));
        settings.SetBooleanValue(settings.APP_PREFERENCES_17_PREDICTION_ENABLED, false);
        for (String mapping : enabledKeyboardMappings) {
            settings.SetBooleanValue(LAYOUT_PREF_PREFIX + mapping, true);
        }

        return new ImeHarness(Robolectric.buildService(TestK12KbIME.class).create().get());
    }

    public TestK12KbIME ime() {
        return ime;
    }

    /** False when onCreate swallowed an exception; see ime().onCreateErrorForTest(). */
    public boolean started() {
        return ime.isStartedForTest();
    }

    /** The field that currently has focus, or null. */
    public FakeTextField field() {
        return field;
    }

    // region Focus

    /** Focuses a new empty plain-text field (TYPE_CLASS_TEXT). */
    public FakeTextField focusText() {
        return focusText(InputType.TYPE_CLASS_TEXT);
    }

    /** Focuses a new empty field of the given inputType. */
    public FakeTextField focusText(int inputType) {
        EditorInfo info = new EditorInfo();
        info.inputType = inputType;
        info.initialSelStart = 0;
        info.initialSelEnd = 0;
        info.packageName = "com.example.editor";
        info.fieldId = 1;
        return focus(info);
    }

    /**
     * Moves focus to a new empty field described by info, in the order
     * InputMethodService.doStartInput uses: finish the previous field's input view
     * and input, then start input and the input view for the new one.
     */
    public FakeTextField focus(EditorInfo info) {
        if (field != null) {
            ime.onFinishInputView(true);
            ime.onFinishInput();
        }
        field = new FakeTextField(ime::onUpdateSelection);
        ime.attachField(field, info);
        ime.onStartInput(info, false);
        ime.onStartInputView(info, false);
        ShadowLooper.idleMainLooper();
        return field;
    }

    // endregion

    // region Keys

    /** Press and release one key, then pause. */
    public void tap(int keyCode) {
        tap(keyCode, 0);
        advance(KEY_GAP_MS);
    }

    /** Two taps of the same key 100 ms apart, then pause. */
    public void doubleTap(int keyCode) {
        tap(keyCode, 0);
        advance(DOUBLE_TAP_INTERVAL_MS - TAP_HOLD_MS);
        tap(keyCode, 0);
        advance(KEY_GAP_MS);
    }

    /** Hold a key for 700 ms with auto-repeat downs every 50 ms, release, then pause. */
    public void longPress(int keyCode) {
        long downTime = now();
        dispatch(new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, 0));
        for (int repeat = 1; repeat * REPEAT_INTERVAL_MS <= LONG_PRESS_HOLD_MS; repeat++) {
            advance(REPEAT_INTERVAL_MS);
            dispatch(new KeyEvent(downTime, now(), KeyEvent.ACTION_DOWN, keyCode, repeat, 0));
        }
        dispatch(new KeyEvent(downTime, now(), KeyEvent.ACTION_UP, keyCode, 0, 0));
        advance(KEY_GAP_MS);
    }

    /** Short tap of the language key (KEY2 mechanics: KEYCODE_0 -> ActionChangeKeyboardLayout). */
    public void languageKey() {
        tap(KeyEvent.KEYCODE_0);
    }

    /**
     * Types on the KEY2 letter keys: a-z tap their key, A-Z tap it with Left Shift
     * held, ' ' taps Space. Other characters have no key of their own on the KEY2
     * and are rejected rather than skipped, so a test cannot silently lose input.
     */
    public void type(String qwerty) {
        for (int i = 0; i < qwerty.length(); i++) {
            char c = qwerty.charAt(i);
            if (c >= 'a' && c <= 'z') {
                tap(KeyEvent.KEYCODE_A + (c - 'a'));
            } else if (c >= 'A' && c <= 'Z') {
                shiftedTap(KeyEvent.KEYCODE_A + (c - 'A'));
            } else if (c == ' ') {
                tap(KeyEvent.KEYCODE_SPACE);
            } else {
                throw new IllegalArgumentException("No KEY2 key types '" + c + "' in \"" + qwerty + "\"");
            }
        }
    }

    /** Left Ctrl held around a tap of the key, e.g. ctrl(KEYCODE_V) for paste. */
    public void ctrl(int keyCode) {
        chord(KeyEvent.KEYCODE_CTRL_LEFT, META_CTRL_LEFT, keyCode);
    }

    /** Left Alt held around a tap of the key. */
    public void alt(int keyCode) {
        chord(KeyEvent.KEYCODE_ALT_LEFT, META_ALT_LEFT, keyCode);
    }

    /** One-shot Alt: tap Left Alt on its own, then tap the key. */
    public void altTap(int keyCode) {
        tap(KeyEvent.KEYCODE_ALT_LEFT);
        tap(keyCode);
    }

    /**
     * The language key (KEYCODE_0) held around a tap of the key. On the KEY2 this is the
     * "key 0 + key" chord of keyboard_mechanics.json, e.g. key0(KEYCODE_DEL) deletes the
     * previous word.
     */
    public void key0(int keyCode) {
        chord(KeyEvent.KEYCODE_0, 0, keyCode);
    }

    /** Puts plain text on the system clipboard, as another app's copy would. */
    public void clipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    // endregion

    // region Internals

    /** Left Shift down, tap the key, Shift up, pause. Shift is held under time-short-press. */
    private void shiftedTap(int keyCode) {
        chord(KeyEvent.KEYCODE_SHIFT_LEFT, META_SHIFT_LEFT, keyCode);
    }

    /**
     * Holding key down (its events and the tapped key's carry holdMeta), tap the key,
     * release, pause. The holding key is held under time-short-press.
     */
    private void chord(int holdingKey, int holdMeta, int keyCode) {
        long holdDown = now();
        dispatch(new KeyEvent(holdDown, holdDown, KeyEvent.ACTION_DOWN, holdingKey, 0, holdMeta));
        advance(TAP_HOLD_MS);
        tap(keyCode, holdMeta);
        advance(TAP_HOLD_MS);
        dispatch(new KeyEvent(holdDown, now(), KeyEvent.ACTION_UP, holdingKey, 0, 0));
        advance(KEY_GAP_MS);
    }

    /** Down, hold TAP_HOLD_MS, up; no pause afterwards. */
    private void tap(int keyCode, int metaState) {
        long downTime = now();
        dispatch(new KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
        advance(TAP_HOLD_MS);
        dispatch(new KeyEvent(downTime, now(), KeyEvent.ACTION_UP, keyCode, 0, metaState));
    }

    /**
     * Gives the event to the IME; a key the IME declines goes on to the app, which
     * applies it to the focused field.
     */
    private void dispatch(KeyEvent event) {
        boolean consumed = event.getAction() == KeyEvent.ACTION_DOWN
                ? ime.onKeyDown(event.getKeyCode(), event)
                : ime.onKeyUp(event.getKeyCode(), event);
        if (!consumed && field != null) {
            field.sendKeyEvent(event);
        }
        ShadowLooper.idleMainLooper();
    }

    private static void advance(long ms) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(ms));
        ShadowLooper.idleMainLooper();
    }

    private static long now() {
        return SystemClock.uptimeMillis();
    }

    // endregion
}
