package gm.ui;

import gm.dto.EventInfoDTO;
import gm.dto.LoadResultDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.TradeRecordDTO;

import java.util.List;
import java.util.Locale;

/**
 * Presents the answers of the engine to the user.
 * <p>
 * This is the only place in the system that writes to the screen. All the decimal numbers are printed
 * with two digits after the point.
 */
class ConsolePrinter {

    private static final String SEPARATOR = "----------------------------------------";
    private static final String NUMBER_FORMAT = "%.2f";

    void printWelcome() {
        System.out.println(SEPARATOR);
        System.out.println("Welcome to Guess Market!");
        System.out.println("Start by loading an events file, and then trade in the events it holds.");
    }

    void printMenu() {
        System.out.println(SEPARATOR);
        System.out.println("Please choose a command:");
        MenuCommand[] commands = MenuCommand.values();
        for (int index = 0; index < commands.length; index++) {
            System.out.println((index + 1) + ") " + commands[index].getDisplayName());
        }
    }

    void printMessage(String message) {
        System.out.println(message);
    }

    void printError(String message) {
        System.out.println("The command could not be completed: " + message);
    }

    void printLoadResult(LoadResultDTO result) {
        System.out.println("The file [" + result.filePath() + "] is valid and was fully loaded into "
                + "the system.");
        System.out.println("Events loaded: " + result.eventsLoaded());
        System.out.println("Total subsidy that was invested in these events: "
                + number(result.totalSubsidy()));
    }

    /**
     * Prints a numbered list of events (the numbering starts at 1), with all their details.
     */
    void printEvents(List<EventInfoDTO> events) {
        System.out.println("The system holds " + events.size() + " event(s):");
        for (int index = 0; index < events.size(); index++) {
            printEvent(index + 1, events.get(index));
        }
    }

    private void printEvent(int listNumber, EventInfoDTO event) {
        System.out.println(listNumber + ") Event number (id): " + event.id());
        System.out.println("   Name: " + event.name());
        System.out.println("   Description: " + event.description());
        System.out.println("   Commission: " + event.commissionPercent() + "% ("
                + event.commissionType().getDisplayName() + ")");
        System.out.println("   Options: " + optionNames(event));
        System.out.println("   Status: " + event.status().getDisplayName());
    }

    private String optionNames(EventInfoDTO event) {
        StringBuilder names = new StringBuilder();
        List<String> optionNames = event.optionNames();
        for (int index = 0; index < optionNames.size(); index++) {
            if (index > 0) {
                names.append("   ");
            }
            names.append(index + 1).append(") ").append(optionNames.get(index));
        }
        return names.toString();
    }

    /**
     * Prints the current state of the options of an event: the value of every option and the amount
     * of shares that were bought from it.
     */
    void printCurrentState(MarketStateDTO state) {
        System.out.println("Current state of the event [" + state.eventInfo().name() + "]:");
        List<OptionStateDTO> optionStates = state.optionStates();
        for (int index = 0; index < optionStates.size(); index++) {
            OptionStateDTO option = optionStates.get(index);
            System.out.println("   " + (index + 1) + ") " + option.name() + " - current value: "
                    + number(option.value()) + " | shares bought so far: " + option.shares());
        }
    }

    /**
     * Prints the full trading state of an event: its options, its account and its history.
     */
    void printMarketState(MarketStateDTO state) {
        EventInfoDTO event = state.eventInfo();
        System.out.println(SEPARATOR);
        System.out.println("Event [" + event.name() + "] (id " + event.id() + ") - "
                + event.status().getDisplayName());
        printCurrentState(state);
        System.out.println("Event account balance: " + number(state.accountBalance()));
        System.out.println("Total commission collected so far: "
                + number(state.totalCommissionCollected()));
        printTradeHistory(state.tradeHistory());
        state.winningOption().ifPresent(winner -> printClosedSummary(state.optionStates(), winner));
    }

    /**
     * Prints the totals-and-decision summary for a closed event in one readable sentence, e.g.
     * "100 Yes, 0 No were bought. Decided: Yes."
     */
    private void printClosedSummary(List<OptionStateDTO> optionStates, String winner) {
        StringBuilder totals = new StringBuilder();
        for (int index = 0; index < optionStates.size(); index++) {
            if (index > 0) {
                totals.append(", ");
            }
            OptionStateDTO option = optionStates.get(index);
            totals.append(option.shares()).append(" ").append(option.name());
        }
        totals.append(" were bought. Decided: ").append(winner).append(".");
        System.out.println(totals);
    }

    private void printTradeHistory(List<TradeRecordDTO> history) {
        if (history.isEmpty()) {
            System.out.println("No shares were bought in this event yet.");
            return;
        }
        System.out.println("Trading history (from the latest purchase to the first one):");
        for (int index = 0; index < history.size(); index++) {
            TradeRecordDTO trade = history.get(index);
            System.out.println("   " + (index + 1) + ") Option: " + trade.optionName()
                    + " | shares: " + trade.shares()
                    + " | price of the shares: " + number(trade.sharesCost())
                    + " | commission: " + number(trade.commissionPaid())
                    + " | total paid: " + number(trade.totalPaid()));
        }
    }

    void printPurchaseResult(PurchaseResultDTO result) {
        System.out.println("The purchase was completed successfully.");
        System.out.println("You bought " + result.shares() + " share(s) of ["
                + result.optionName() + "].");
        System.out.println("Price of the shares: " + number(result.sharesCost()));
        System.out.println("Commission: " + number(result.commissionPaid()));
        System.out.println("Total amount paid: " + number(result.totalPaid()));
        printMarketState(result.stateAfterPurchase());
    }

    private String number(double value) {
        return String.format(Locale.US, NUMBER_FORMAT, value);
    }
}
