package gm.dto;

import java.util.List;

/**
 * The value of a single option of an event over time.
 *
 * @param optionName name of the option
 * @param points     one point for every moment the value could change, in the order they happened;
 *                   empty as long as the option never had a price
 */
public record PriceHistoryDTO(String optionName, List<HistoryPointDTO> points) {

    public PriceHistoryDTO {
        points = List.copyOf(points);
    }
}
