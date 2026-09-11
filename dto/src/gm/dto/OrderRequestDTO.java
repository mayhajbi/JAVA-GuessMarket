package gm.dto;

/**
 * A request to place an order in the order book of an event.
 *
 * @param eventId     id of the event
 * @param userName    name of the user who places the order
 * @param side        buy or sell
 * @param optionIndex zero based index of the option whose shares are traded
 * @param quantity    amount of shares, must be positive
 * @param price       price per share, in whole cents, between 0.01 and the base value (d) minus 0.01
 */
public record OrderRequestDTO(int eventId,
                              String userName,
                              OrderSide side,
                              int optionIndex,
                              long quantity,
                              double price) {
}
