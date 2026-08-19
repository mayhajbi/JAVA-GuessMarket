package gm.dto;

import java.util.List;

/**
 * General details of a single event, as presented to the user.
 *
 * @param id                event unique number, as given in the data file
 * @param name              event name
 * @param description       free text describing the event and its closing condition
 * @param commissionPercent commission of the event, in percent (0 - 90)
 * @param commissionType    the way the commission is collected
 * @param optionNames       the possible outcomes of the event, in their original order
 * @param status            active or closed
 */
public record EventInfoDTO(int id,
                           String name,
                           String description,
                           int commissionPercent,
                           CommissionType commissionType,
                           List<String> optionNames,
                           EventStatus status) {

    public EventInfoDTO {
        optionNames = List.copyOf(optionNames);
    }
}
