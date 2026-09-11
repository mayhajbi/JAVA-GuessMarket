package gm.engine.api;

import gm.dto.EventInfoDTO;
import gm.dto.LoadResultDTO;
import gm.dto.MarketStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserInfoDTO;

import java.util.List;

/**
 * The complete set of abilities the Guess Market engine offers to a user interface layer.
 * <p>
 * The engine is passive: it only answers requests, and it does not know who is calling it. Every
 * answer is returned as an immutable data transfer object, so that no caller can reach or change the
 * inner state of the system.
 * <p>
 * Every failure is reported by throwing an unchecked
 * {@link gm.engine.exception.GuessMarketException}, which carries a detailed message that can be
 * presented to the user as is.
 * <p>
 * Note about numbering: options are identified by a zero based index, exactly as they appear in the
 * data file. A user interface that presents them to the user starting from 1 has to subtract 1
 * before calling the engine.
 */
public interface GuessMarketEngine {

    /**
     * Loads a data file into the system. A file is loaded only if it is completely valid, so a failed
     * load never harms the data that is already in the system.
     *
     * @param xmlFilePath full path of the XML file
     * @return details of the load that was performed
     */
    LoadResultDTO loadEventsFile(String xmlFilePath);

    /**
     * @return general details of all the events in the system, in the order of the data file
     */
    List<EventInfoDTO> getAllEvents();

    /**
     * @return general details of the events that are still active
     */
    List<EventInfoDTO> getActiveEvents();

    /**
     * @return general details of all the users in the system, in the order of the data file
     */
    List<UserInfoDTO> getAllUsers();

    /**
     * @param eventId id of the requested event
     * @return the full trading state of that event
     */
    MarketStateDTO getMarketState(int eventId);

    /**
     * Opens an inactive event for trading. Only the market maker of the event may open it, and it
     * pays the initial subsidy from the own account.
     *
     * @param eventId  id of the event to open
     * @param userName name of the user asking to open it
     * @return the state of the event right after it was opened
     */
    MarketStateDTO openEvent(int eventId, String userName);

    /**
     * Buys shares of one of the options of an active event, on behalf of a user.
     *
     * @param eventId     id of the event to trade in
     * @param userName    name of the buying user
     * @param optionIndex zero based index of the option to buy
     * @param quantity    amount of shares to buy, must be positive
     * @return what was paid, the balance of the buyer, and the state of the event right after the
     *         purchase
     */
    PurchaseResultDTO buyShares(int eventId, String userName, int optionIndex, long quantity);

    /**
     * Closes an active event, decides its winning option, pays the winners and hands the commission
     * and what is left in the event account to the market maker. Only the market maker may close it.
     *
     * @param eventId            id of the event to close
     * @param userName           name of the user asking to close it
     * @param winningOptionIndex zero based index of the winning option
     * @return the final state of the event
     */
    MarketStateDTO closeEvent(int eventId, String userName, int winningOptionIndex);

    /**
     * Saves the whole current state of the system into a file, so that it can be loaded again later.
     *
     * @param pathWithoutExtension full path of the target file, without an extension
     * @return the full path of the file that was created
     */
    String saveSystemState(String pathWithoutExtension);

    /**
     * Loads a state that was previously saved by
     * {@link #saveSystemState(String)}. The state replaces the current content of the system, and
     * only if it was read successfully.
     *
     * @param pathWithoutExtension full path of the state file, without an extension
     * @return the full path of the file that was read
     */
    String loadSystemState(String pathWithoutExtension);
}
