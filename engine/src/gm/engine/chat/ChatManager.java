package gm.engine.chat;

import gm.engine.exception.InvalidChatLineException;
import gm.engine.util.InputText;

import java.util.ArrayList;
import java.util.List;

/**
 * The chat of the users: every line that was written, in the order they were written. The version of
 * the chat is the amount of its lines, so a client that knows a version asks only for the lines that
 * came after it.
 * <p>
 * Like the rest of the engine it is not thread safe: the server reaches it under its single lock.
 */
public class ChatManager {

    private final List<ChatLine> lines = new ArrayList<>();

    /**
     * Adds a line to the chat. The text is cleaned like every other text of the system, and has to be
     * written in English.
     *
     * @throws InvalidChatLineException when the line is empty or is not written in English
     */
    public void addLine(String userName, String text) {
        String line = InputText.normalize(text);
        if (line.isEmpty()) {
            throw InvalidChatLineException.empty();
        }
        if (!InputText.isEnglish(line)) {
            throw InvalidChatLineException.notEnglish();
        }
        lines.add(new ChatLine(userName, line));
    }

    /**
     * @param fromVersion the version the caller already has; a version the chat never had (for example of
     *                    a client that outlived a restart of the server) gets the whole chat
     * @return the lines that were written after that version, in the order they were written
     */
    public List<ChatLine> getLines(int fromVersion) {
        int from = fromVersion < 0 || fromVersion > lines.size() ? 0 : fromVersion;
        return new ArrayList<>(lines.subList(from, lines.size()));
    }

    public int getVersion() {
        return lines.size();
    }
}
