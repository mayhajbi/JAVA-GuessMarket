package gm.dto;

import java.util.Optional;

/**
 * What the trading state of every event holds, whatever its trading method: its general details, the
 * commission it collected and its winning option.
 */
public interface EventStateDTO {

    EventInfoDTO eventInfo();

    double totalCommissionCollected();

    /**
     * @return the winning option, empty while the event is not closed
     */
    Optional<String> winningOption();
}
