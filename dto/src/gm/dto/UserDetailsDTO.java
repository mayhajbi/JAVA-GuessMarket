package gm.dto;

import java.util.List;

/**
 * The full details of a single user.
 *
 * @param name    user name
 * @param balance current balance of the user account
 * @param blocked whether the user is blocked from starting further actions
 * @param events  every event the user is the market maker of or has traded in, in the order of the
 *                data file
 */
public record UserDetailsDTO(String name, double balance, boolean blocked, List<UserEventDTO> events) {

    public UserDetailsDTO {
        events = List.copyOf(events);
    }
}
