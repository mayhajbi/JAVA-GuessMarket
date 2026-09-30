package gm.dto;

/**
 * What the other users of the system may see of a user: the name, the balance and whether the user is a
 * market maker, and nothing more.
 *
 * @param name        user name
 * @param balance     current balance of the user account
 * @param marketMaker whether the user is the market maker of at least one event
 */
public record UserSummaryDTO(String name, double balance, boolean marketMaker) {
}
