package gm.engine.util;

/**
 * Small text-cleanup helpers shared by the engine classes that read raw, user-typed strings (file
 * paths in particular). Kept here, inside the engine, so this logic exists in exactly one place and
 * a future UI layer (a different exercise, a GUI, ...) does not need to reimplement it.
 */
public final class InputText {

    private InputText() {
    }

    /**
     * Removes a single pair of surrounding double quotes, if present, so a path that was copied from
     * the file explorer (which wraps it in quotes) can be used as is.
     */
    public static String stripSurroundingQuotes(String text) {
        if (text != null && text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }
}
