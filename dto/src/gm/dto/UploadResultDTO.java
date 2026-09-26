package gm.dto;

import java.util.List;

/**
 * What a file of events added to the system.
 *
 * @param fileName   the name of the file that was uploaded
 * @param eventNames the names of the events the file added, in the order of the file
 */
public record UploadResultDTO(String fileName, List<String> eventNames) {

    public UploadResultDTO {
        eventNames = List.copyOf(eventNames);
    }
}
