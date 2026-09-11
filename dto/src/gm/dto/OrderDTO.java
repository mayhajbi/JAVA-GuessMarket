package gm.dto;

/**
 * An order that is waiting in an order book.
 *
 * @param userName name of the user who placed the order
 * @param quantity amount of shares that are still waiting to be matched
 * @param price    price per share
 */
public record OrderDTO(String userName, long quantity, double price) {
}
