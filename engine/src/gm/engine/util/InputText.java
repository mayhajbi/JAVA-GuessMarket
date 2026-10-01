package gm.engine.util;

import java.util.Locale;

/**
 * Small text-cleanup helpers shared by the engine classes that read raw strings - a value out of a
 * data file, a value a user typed, or a file path. Kept here, inside the engine, so this logic exists
 * in exactly one place and a future UI layer (a different exercise, a GUI, ...) does not need to
 * reimplement it.
 */
public final class InputText {

    private InputText() {
    }

    /**
     * Cleans a text value: spaces at its edges are removed, and a sequence of spaces, tabs or line
     * breaks inside it (a text that was written on several lines) becomes a single space.
     *
     * @return the clean text, or an empty text for {@code null}
     */
    public static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Every input of the system has to be in English: the text may hold only characters of the basic
     * (ASCII) set - English letters, digits, punctuation marks and white space.
     *
     * @return whether the text is written in English only; a {@code null} text is
     */
    public static boolean isEnglish(String value) {
        return value == null || value.chars().allMatch(character -> character < 128);
    }

    /**
     * Cleans a file path the user gave: spaces at its edges are removed, and so is a single pair of
     * surrounding double quotes, so a path that was copied from the file explorer (which wraps it in
     * quotes) can be used as is.
     *
     * @return the clean path, or an empty text for {@code null}
     */
    public static String cleanPath(String rawPath) {
        String path = rawPath == null ? "" : rawPath.trim();
        if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1).trim();
        }
        return path;
    }

    /**
     * @return whether the path ends with the given extension, ignoring case
     */
    public static boolean hasExtension(String path, String extension) {
        return path.toLowerCase(Locale.ROOT).endsWith(extension);
    }

    /**
     * The message for a path that cannot be a file path on this computer at all.
     */
    public static String illegalPathMessage(String path) {
        return "The path '" + path + "' is not a legal file path on this computer. Please check it and "
                + "try again.";
    }
}
