package gm.dto;

import java.util.List;

/**
 * The lines of the chat a client does not have yet, and the version of the chat they bring it to.
 *
 * @param lines   the lines that were written since the version the client asked from, in the order
 *                they were written
 * @param version the version of the chat right now, which is the amount of lines written so far;
 *                the client sends it back the next time, to get only what is new
 */
public record ChatLinesDTO(List<ChatLineDTO> lines, int version) {

    public ChatLinesDTO {
        lines = List.copyOf(lines);
    }
}
