package gm.engine.api;

import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.HistoryPointDTO;
import gm.dto.LoadResultDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserDetailsDTO;
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
     * @param filter the selected types, statuses and commission methods
     * @return general details of the events that match all three selections, in the order of the
     *         data file
     */
    List<EventInfoDTO> getEvents(EventFilterDTO filter);

    /**
     * @return general details of all the users in the system, in the order of the data file
     */
    List<UserInfoDTO> getAllUsers();

    /**
     * @param userName name of the requested user
     * @return the details of that user, including every event the user is the market maker of or
     *         has traded in
     */
    UserDetailsDTO getUserDetails(String userName);

    /**
     * @param eventId id of the requested LMSR event
     * @return the full trading state of that event
     */
    MarketStateDTO getMarketState(int eventId);

    /**
     * @param eventId id of the requested order book event
     * @return the order books, statistics, participants and trades of that event
     */
    OrderBookStateDTO getOrderBookState(int eventId);

    /**
     * The value of every option of an event over time: the value right after the event was opened,
     * and one point for every action that could change it, until the event was closed.
     *
     * @param eventId id of the requested event
     * @return one series of points per option, in the order of the options
     */
    List<PriceHistoryDTO> getEventPriceHistory(int eventId);

    /**
     * The balance of a user over time: the amount the user started with, and one point for every
     * change since then.
     *
     * @param userName name of the requested user
     * @return the points, in the order they happened
     */
    List<HistoryPointDTO> getUserBalanceHistory(String userName);

    /**
     * Opens an inactive event for trading. Only the market maker of the event may open it, and it
     * pays from the own account: the initial subsidy of an LMSR event, or the initial investment of an
     * order book event (receiving the initial pairs of shares).
     *
     * @param eventId  id of the event to open
     * @param userName name of the user asking to open it
     * @return the details of the event right after it was opened
     */
    EventInfoDTO openEvent(int eventId, String userName);

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
     * Places an order in the order book of an active order book event, on behalf of a user, and
     * matches it right away against the waiting orders.
     *
     * @param request the event, user, side, option, quantity and price of the order
     * @return the trades the order created, the balance of the user, and the state of the event right
     *         after the order
     */
    OrderResultDTO submitOrder(OrderRequestDTO request);

    /**
     * Closes an active event, decides its winning option, pays the winners and hands the commission
     * and what is left in the event account to the market maker. Only the market maker may close it.
     *
     * @param eventId            id of the event to close
     * @param userName           name of the user asking to close it
     * @param winningOptionIndex zero based index of the winning option
     * @return the details of the event after it was closed
     */
    EventInfoDTO closeEvent(int eventId, String userName, int winningOptionIndex);

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
