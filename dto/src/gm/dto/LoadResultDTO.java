package gm.dto;

/**
 * The result of a successful load of a data file into the system.
 *
 * @param filePath      the file that was loaded
 * @param eventsLoaded  amount of events that were loaded from the file
 * @param totalSubsidy  total subsidy that was invested in all the LMSR events of the file
 */
public record LoadResultDTO(String filePath, int eventsLoaded, double totalSubsidy) {
}
