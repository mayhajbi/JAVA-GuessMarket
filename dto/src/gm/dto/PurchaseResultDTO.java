package gm.dto;

/**
 * The result of a single purchase of shares in an event.
 *
 * @param optionName          name of the option that was bought
 * @param shares              amount of shares that were bought
 * @param sharesCost          the price of the shares themselves
 * @param commissionPaid      the commission that was added to the payment (0 when the commission of
 *                            the event is collected on close)
 * @param totalPaid           the total amount that was paid
 * @param stateAfterPurchase  the state of the event right after the purchase
 */
public record PurchaseResultDTO(String optionName,
                                long shares,
                                double sharesCost,
                                double commissionPaid,
                                double totalPaid,
                                MarketStateDTO stateAfterPurchase) {
}
