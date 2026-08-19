package gm.dto;

import java.util.List;
import java.util.Optional;

/**
 * A full picture of the trading state of a single event.
 *
 * @param eventInfo                general details of the event
 * @param optionStates             the current state of every option of the event
 * @param accountBalance           the current balance of the event (market maker) account
 * @param totalCommissionCollected total commission that was actually collected until now
 * @param tradeHistory             the trading history of the event, latest trade first
 * @param winningOptionName        the winning option, {@code null} while the event is still active
 */
public record MarketStateDTO(EventInfoDTO eventInfo,
                             List<OptionStateDTO> optionStates,
                             double accountBalance,
                             double totalCommissionCollected,
                             List<TradeRecordDTO> tradeHistory,
                             String winningOptionName) {

    public MarketStateDTO {
        optionStates = List.copyOf(optionStates);
        tradeHistory = List.copyOf(tradeHistory);
    }

    public Optional<String> winningOption() {
        return Optional.ofNullable(winningOptionName);
    }
}
