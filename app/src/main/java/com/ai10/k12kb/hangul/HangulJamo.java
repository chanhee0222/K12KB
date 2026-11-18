package com.ai10.k12kb.hangul;

/** Соответствие символов совместимых джамо (ㄱ…ㅎ, ㅏ…ㅣ) значениям {@link SemanticJamo}. */
public final class HangulJamo {
    private static final SemanticJamo[] CONSONANTS = new SemanticJamo[19];
    private static final SemanticJamo[] VOWELS = new SemanticJamo[21];

    static {
        for (int i = 0; i < 19; i++) {
            CONSONANTS[i] = SemanticJamo.contextualConsonant(i);
        }
        for (int i = 0; i < 21; i++) {
            VOWELS[i] = SemanticJamo.vowel(i);
        }
    }

    private static final char[] CONSONANT_CHARS = new char[19];
    private static final char[] VOWEL_CHARS = new char[21];

    static {
        for (int i = 0; i < 19; i++) {
            CONSONANT_CHARS[i] = HangulTables.choJamo(i).charAt(0);
        }
        for (int i = 0; i < 21; i++) {
            VOWEL_CHARS[i] = HangulTables.jungJamo(i).charAt(0);
        }
    }

    private HangulJamo() {
    }

    /** Джамо для символа или null, если это не одиночная начальная согласная и не гласная. */
    public static SemanticJamo fromChar(char c) {
        for (int i = 0; i < CONSONANT_CHARS.length; i++) {
            if (CONSONANT_CHARS[i] == c) {
                return CONSONANTS[i];
            }
        }
        for (int i = 0; i < VOWEL_CHARS.length; i++) {
            if (VOWEL_CHARS[i] == c) {
                return VOWELS[i];
            }
        }
        return null;
    }

    public static boolean isJamo(char c) {
        return fromChar(c) != null;
    }
}
