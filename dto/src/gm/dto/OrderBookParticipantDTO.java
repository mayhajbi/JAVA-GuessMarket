package gm.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The state of a single participant in an order book event: a user who holds shares or placed an
 * order, even one that was never matched. Every list holds one value per option, in the order of
 * the options.
 *
 * @param userName              name of the user
 * @param sharesPerOption       amount of shares held
 * @param holdingValuePerOption the current value of the shares held; an element is {@code null}
 *                              when the option has no price yet
 * @param paidPerOption         the money paid for shares bought in trades (without commission)
 * @param initialInvestmentPaid the money paid for the initial pairs of shares (market maker only)
 * @param commissionPaid        total commission paid, on purchases and on the payout
 * @param received              total money received: from selling shares and from the payout
 * @param profitOrLoss          everything received minus everything paid; final once the event is
 *                              closed
 */
public record OrderBookParticipantDTO(String userName,
                                      List<Long> sharesPerOption,
                                      List<Double> holdingValuePerOption,
                                      List<Double> paidPerOption,
                                      double initialInvestmentPaid,
                                      double commissionPaid,
                                      double received,
                                      double profitOrLoss) {

    public OrderBookParticipantDTO {
        sharesPerOption = List.copyOf(sharesPerOption);
        holdingValuePerOption = Collections.unmodifiableList(new ArrayList<>(holdingValuePerOption));
        paidPerOption = List.copyOf(paidPerOption);
    }
}
