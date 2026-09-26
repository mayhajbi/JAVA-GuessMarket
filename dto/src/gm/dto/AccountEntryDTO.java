package gm.dto;

/**
 * One movement of money in the account of a user, as shown in the list of the account.
 *
 * @param type         the kind of the movement
 * @param amount       what came in (positive) or went out (negative)
 * @param balanceAfter the balance of the account right after the movement
 */
public record AccountEntryDTO(AccountEntryType type, double amount, double balanceAfter) {
}
