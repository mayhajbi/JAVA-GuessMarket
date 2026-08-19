package gm.ui;

import gm.dto.EventInfoDTO;
import gm.dto.MarketStateDTO;
import gm.engine.api.GuessMarketEngine;
import gm.engine.exception.GuessMarketException;
import gm.engine.impl.GuessMarketEngineImpl;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * The console application of Guess Market.
 * <p>
 * This class drives the whole system: it presents the menu, collects the input of the user, asks the
 * engine to do the work and presents the answers. It talks to the engine only through the
 * {@link GuessMarketEngine} interface.
 */
public class GuessMarketConsoleApp {

    private final GuessMarketEngine engine;
    private final ConsoleInput input = new ConsoleInput();
    private final ConsolePrinter printer = new ConsolePrinter();

    public GuessMarketConsoleApp(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /**
     * The only place in the system that knows the concrete implementation of the engine.
     */
    public static void main(String[] args) {
        new GuessMarketConsoleApp(new GuessMarketEngineImpl()).run();
    }

    public void run() {
        printer.printWelcome();
        boolean keepRunning = true;
        while (keepRunning) {
            try {
                printer.printMenu();
                int choice = input.readNumberInRange("Please enter the number of your choice:", 1,
                        MenuCommand.values().length);
                keepRunning = execute(MenuCommand.values()[choice - 1]);
            } catch (GuessMarketException exception) {
                printer.printError(exception.getMessage());
            } catch (NoSuchElementException exception) {
                printer.printMessage("There is no more input to read. Exiting Guess Market.");
                keepRunning = false;
            } catch (RuntimeException exception) {
                printer.printError("an unexpected problem occurred (" + exception + "). The system "
                        + "keeps running, please try again.");
            }
        }
    }

    /**
     * @return false when the user asked to leave the system
     */
    private boolean execute(MenuCommand command) {
        switch (command) {
            case LOAD_EVENTS_FILE -> loadEventsFile();
            case SHOW_EVENTS -> showEvents();
            case SHOW_MARKET_STATE -> showMarketState();
            case PARTICIPATE -> participateInEvent();
            case CLOSE_EVENT -> closeEvent();
            case SAVE_SYSTEM_STATE -> saveSystemState();
            case LOAD_SYSTEM_STATE -> loadSystemState();
            case EXIT -> {
                printer.printMessage("Thank you for using Guess Market. Goodbye!");
                return false;
            }
        }
        return true;
    }

    private void loadEventsFile() {
        String path = input.readText("Please enter the full path of the XML events file "
                + "(for example: C:\\my files\\events.xml):");
        printer.printLoadResult(engine.loadEventsFile(path));
    }

    private void showEvents() {
        printer.printEvents(engine.getAllEvents());
    }

    private void showMarketState() {
        List<EventInfoDTO> events = engine.getAllEvents();
        if (events.isEmpty()) {
            printer.printMessage("There are no events in the system.");
            return;
        }
        printer.printEvents(events);
        EventInfoDTO chosenEvent = chooseEvent(events, "Please enter the number of the event whose "
                + "trading state you would like to see:");
        printer.printMarketState(engine.getMarketState(chosenEvent.id()));
    }

    private void participateInEvent() {
        List<EventInfoDTO> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            printer.printMessage("There are no active events in the system, so there is nothing to "
                    + "participate in right now.");
            return;
        }
        printer.printEvents(activeEvents);
        EventInfoDTO chosenEvent = chooseEvent(activeEvents, "Please enter the number of the event "
                + "you would like to participate in:");

        printer.printCurrentState(engine.getMarketState(chosenEvent.id()));
        int optionNumber = input.readNumberInRange("Please enter the number of the option you believe "
                + "in:", 1, chosenEvent.optionNames().size());
        long shares = input.readPositiveAmount("Please enter the amount of shares you would like to "
                + "buy:");

        printer.printPurchaseResult(engine.buyShares(chosenEvent.id(), optionNumber - 1, shares));
    }

    private void closeEvent() {
        List<EventInfoDTO> activeEvents = engine.getActiveEvents();
        if (activeEvents.isEmpty()) {
            printer.printMessage("There are no active events in the system, so there is nothing to "
                    + "close right now.");
            return;
        }
        printer.printEvents(activeEvents);
        EventInfoDTO chosenEvent = chooseEvent(activeEvents, "Please enter the number of the event "
                + "you would like to close:");

        printer.printMarketState(engine.getMarketState(chosenEvent.id()));
        int winningOptionNumber = input.readNumberInRange("Please enter the number of the option the "
                + "event ended with:", 1, chosenEvent.optionNames().size());

        MarketStateDTO finalState = engine.closeEvent(chosenEvent.id(), winningOptionNumber - 1);
        printer.printMessage("The event was closed successfully. Here is its summary:");
        printer.printMarketState(finalState);
    }

    private void saveSystemState() {
        String path = input.readText("Please enter the full path of the file to save the state into, "
                + "without an extension (for example: C:\\my files\\my-market):");
        printer.printMessage("The state of the system was saved successfully into ["
                + engine.saveSystemState(path) + "].");
    }

    private void loadSystemState() {
        String path = input.readText("Please enter the full path of the saved state file, without an "
                + "extension (for example: C:\\my files\\my-market):");
        printer.printMessage("The state of the system was loaded successfully from ["
                + engine.loadSystemState(path) + "].");
        printer.printEvents(engine.getAllEvents());
    }

    private EventInfoDTO chooseEvent(List<EventInfoDTO> events, String prompt) {
        int eventNumber = input.readNumberInRange(prompt, 1, events.size());
        return events.get(eventNumber - 1);
    }
}
