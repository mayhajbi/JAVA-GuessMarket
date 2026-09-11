package gm.dto;

/**
 * A single trade in an order book event, from the side of the buyer.
 *
 * @param buyerName        name of the user who bought the shares
 * @param counterpartyName the seller of the shares, or - for minted shares - the user who bought the
 *                         shares of the other option in the same mint
 * @param optionName       name of the option that was bought
 * @param quantity         amount of shares
 * @param price            price per share that the buyer paid
 * @param commissionPaid   the commission the buyer paid on top of the price
 * @param minted           whether the shares were newly minted (rather than sold by another user)
 */
public record OrderBookTradeDTO(String buyerName,
                                String counterpartyName,
                                String optionName,
                                long quantity,
                                double price,
                                double commissionPaid,
                                boolean minted) {
}
