package com.ai10.k12kb.testing;

import android.content.Context;

import com.ai10.k12kb.prediction.PredictionEngine;
import com.ai10.k12kb.prediction.WordPredictor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stands in for NativeSymSpellEngine, whose dictionaries are built by native code that does not
 * load on the JVM. Everything else on the prediction path (WordPredictor, SuggestionBar, the
 * IME's wiring) is the real code. The dictionary is always ready, so WordPredictor never starts a
 * load thread.
 *
 * Suggestions are predictable: for a prefix "hel" it offers "hel+"; with no prefix after the word
 * "hel" (next-word prediction) it offers "after-hel"; with neither it offers nothing.
 */
public final class FakePredictionEngine implements PredictionEngine {

    /** Every suggest() call, as "prefix|previousWord". */
    public final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile String loadedLocale = "en";

    @Override
    public List<WordPredictor.Suggestion> suggest(String input, String previousWord, int limit) {
        requests.add(input + "|" + previousWord);
        List<WordPredictor.Suggestion> result = new ArrayList<>();
        if (!input.isEmpty()) {
            result.add(new WordPredictor.Suggestion(input + "+", 0, 1.0));
        } else if (previousWord != null && !previousWord.isEmpty()) {
            result.add(new WordPredictor.Suggestion("after-" + previousWord, 0, 1.0));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void loadDictionary(Context context, String locale) {
        loadedLocale = locale;
    }

    @Override
    public void preloadDictionary(Context context, String locale) {
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public String getLoadedLocale() {
        return loadedLocale;
    }

    @Override
    public void setReadyListener(ReadyListener listener) {
    }

    @Override
    public void closeAll() {
    }
}
