package com.ai10.k12kb.hangul;

/** Что за редактор перед нами, по его EditorInfo. */
public final class EditorKind {
    /** InputType.TYPE_MASK_CLASS; литералом, чтобы класс проверялся на голой JVM. */
    private static final int TYPE_MASK_CLASS = 0x0000000f;

    private EditorKind() {
    }

    /**
     * Терминал (Termux и подобные): у inputType нет битов класса (TYPE_NULL; и режим Termux
     * "enforce char-based input", VISIBLE_PASSWORD | NO_SUGGESTIONS, тоже без класса), и редактор
     * не сообщает курсор (initialSelStart &lt; 0). Отправленное туда назад не забрать.
     */
    public static boolean isTerminal(int inputType, int initialSelStart) {
        return (inputType & TYPE_MASK_CLASS) == 0 && initialSelStart < 0;
    }
}
