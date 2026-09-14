package gm.dto;

import java.util.List;
import java.util.Optional;

/**
 * A full picture of the trading state of a single order book event.
 *
 * @param eventInfo                general details of the event
 * @param baseValue                the base value (d): the payout of a winning share
 * @param mintAllowed              whether new pairs of shares may be minted
 * @param initialInvestment        the amount the market maker invests when opening the event
 * @param options                  the order book and statistics of every option
 * @param participants             every user who holds shares or placed an order
 * @param tradeHistory             the trades of the event, latest first
 * @param totalCommissionCollected total commission the market maker collected until now
 * @param winningOptionName        the winning option, {@code null} while the event is not closed
 */
public record OrderBookStateDTO(EventInfoDTO eventInfo,
                                int baseValue,
                                boolean mintAllowed,
                                int initialInvestment,
                                List<OrderBookOptionDTO> options,
                                List<OrderBookParticipantDTO> participants,
                                List<OrderBookTradeDTO> tradeHistory,
                                double totalCommissionCollected,
                                String winningOptionName) implements EventStateDTO {

    public OrderBookStateDTO {
        options = List.copyOf(options);
        participants = List.copyOf(participants);
        tradeHistory = List.copyOf(tradeHistory);
    }

    public Optional<String> winningOption() {
        return Optional.ofNullable(winningOptionName);
    }
}
