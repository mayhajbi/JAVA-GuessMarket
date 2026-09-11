package gm.dto;

/**
 * A single line in the trading history of an event.
 *
 * @param buyerName      name of the user who made the purchase
 * @param optionName     name of the option that was bought
 * @param shares         amount of shares that were bought
 * @param sharesCost     the price that was paid for the shares themselves
 * @param commissionPaid the commission that was paid on top of the shares price (0 when the
 *                       commission of the event is collected on close)
 * @param totalPaid      the total amount that was paid by the buyer
 */
public record TradeRecordDTO(String buyerName,
                             String optionName,
                             long shares,
                             double sharesCost,
                             double commissionPaid,
                             double totalPaid) {
}
