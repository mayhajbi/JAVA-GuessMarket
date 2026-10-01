package gm.dto;

/**
 * A single line of the chat.
 *
 * @param userName   the name of the user who wrote the line
 * @param timeMillis the moment the server received the line
 * @param text       what the user wrote
 */
public record ChatLineDTO(String userName, long timeMillis, String text) {
}
