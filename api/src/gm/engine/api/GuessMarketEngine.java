package gm.engine.api;

import gm.dto.AccountEntryDTO;
import gm.dto.ChatLinesDTO;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.NewEventRequestDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UploadResultDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserInfoDTO;

import java.io.InputStream;
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
 * file of the event. A user interface that presents them to the user starting from 1 has to subtract 1
 * before calling the engine.
 */
public interface GuessMarketEngine {

    /**
     * @return general details of all the events in the system, in the order they were added
     */
    List<EventInfoDTO> getAllEvents();

    /**
     * @param filter the selected types, statuses and commission methods
     * @return general details of the events that match all three selections, in the order they were
     *         added
     */
    List<EventInfoDTO> getEvents(EventFilterDTO filter);

    /**
     * @return general details of all the users in the system, in the order they registered
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
     * Registers a new user, who starts with an empty account.
     *
     * @param userName the name the user asked for; names are unique, compared without case
     * @return the details of the new user
     */
    UserInfoDTO registerUser(String userName);

    /**
     * Adds money to the account of a user. Depositing is always possible, also for a blocked user; once
     * the balance is back to zero or more, the user is not blocked any more.
     *
     * @param userName name of the user
     * @param amount   the amount to add, must be positive
     * @return the details of the user after the deposit
     */
    UserInfoDTO deposit(String userName, double amount);

    /**
     * Adds the events of a file to the ones already in the system, and makes the user their market
     * maker. Nothing is replaced. The whole file is checked
     * first, and if anything in it is wrong no event of it is added. The content is only read, never
     * stored.
     *
     * @param userName name of the user who uploads the file
     * @param fileName the name of the file, as the user knows it
     * @param content  the content of the file; the caller closes it
     * @return the name of the file and the names of the events it added
     */
    UploadResultDTO uploadEvents(String userName, String fileName, InputStream content);

    /**
     * Every movement of money in the account of a user, from the latest to the first one.
     *
     * @param userName name of the requested user
     * @return the movements, each with its kind, its amount and the balance right after it
     */
    List<AccountEntryDTO> getAccountEntries(String userName);

    /**
     * Creates a new event on behalf of a user, who becomes its market maker (bonus). The event is
     * created inactive and gets an id no other event uses; the same user then opens it like any
     * other event of theirs. Every rule a data file has to obey is checked here as well.
     *
     * @param request the details of the event, including the fields of the chosen trading method
     * @return the details of the event that was created
     */
    EventInfoDTO createEvent(NewEventRequestDTO request);

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
     * @return what was bought, what was paid for it, and the balance of the buyer right after the purchase
     */
    PurchaseResultDTO buyShares(int eventId, String userName, int optionIndex, long quantity);

    /**
     * What {@link #buyShares} would do right now, without doing it: the same checks, the same price and
     * the same commission, and nothing changes. The price depends on the shares that exist, so a trade
     * of another user in between can change it.
     *
     * @return the purchase as it would be: in {@code buyerBalance} the balance the buyer would have
     *         afterwards, and in {@code buyerBlocked} whether that would block the buyer
     */
    PurchaseResultDTO quoteShares(int eventId, String userName, int optionIndex, long quantity);

    /**
     * Places an order in the order book of an active order book event, on behalf of a user, and
     * matches it right away against the waiting orders.
     *
     * @param request the event, user, side, option, quantity and price of the order
     * @return the trades the order created, how much of it was matched and how much waits, and the
     *         balance of the user right after the order
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
     * Adds a line to the chat all the users share (bonus). The line has to hold a text, in English.
     *
     * @param userName name of the user who writes the line
     * @param text     what the user wrote
     */
    void sendChatLine(String userName, String text);

    /**
     * The lines of the chat that were written after the version the caller already has (bonus).
     *
     * @param fromVersion the version of the chat the caller has, 0 for a caller that has nothing yet
     * @return the new lines, in the order they were written, and the version of the chat right now
     */
    ChatLinesDTO getChatLines(int fromVersion);
}
