package gm.dto;

/**
 * The result of a successful load of a data file into the system.
 *
 * @param filePath      the file that was loaded
 * @param eventsLoaded  amount of events that were loaded from the file
 * @param usersLoaded   amount of users that were loaded from the file
 * @param totalSubsidy  total subsidy the market makers of the LMSR events of the file will have to
 *                      invest when they open their events
 */
public record LoadResultDTO(String filePath, int eventsLoaded, int usersLoaded, double totalSubsidy) {
}
