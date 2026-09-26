package gm.dto;

/**
 * General details of a single user, as presented to the user of the application.
 *
 * @param name        user name
 * @param balance     current balance of the user account
 * @param blocked     whether the user is blocked from starting further actions (the balance is
 *                    below zero right now)
 * @param marketMaker whether the user is the market maker of at least one event
 */
public record UserInfoDTO(String name, double balance, boolean blocked, boolean marketMaker) {
}
