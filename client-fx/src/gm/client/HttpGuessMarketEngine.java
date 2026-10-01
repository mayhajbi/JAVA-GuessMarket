package gm.client;

import com.google.gson.Gson;
import gm.dto.AccountEntryDTO;
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
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The engine of the client: every method is a request to the server and the answer is turned back into the
 * DTO the screens of exercise 2 already work with. The user who acts is always the one who logged in -
 * the server knows it from the session - so the user names in the signatures are not sent.
 */
public class HttpGuessMarketEngine implements GuessMarketEngine {

    private static final Map<String, String> NO_PARAMS = Map.of();

    private record BuyBody(int eventId, int optionIndex, long quantity) {
    }

    private final HttpApi api;
    private final Gson gson = new Gson();

    /**
     * @param api the connection to the server, shared with the parts of the client that refresh themselves
     */
    public HttpGuessMarketEngine(HttpApi api) {
        this.api = api;
    }

    @Override
    public List<EventInfoDTO> getAllEvents() {
        return pull(Query.allEvents());
    }

    @Override
    public List<EventInfoDTO> getEvents(EventFilterDTO filter) {
        return pull(Query.events(filter));
    }

    /**
     * The user who is logged in comes first, followed by the other users, of whom the server tells only the
     * name, the balance and whether they are a market maker.
     */
    @Override
    public List<UserInfoDTO> getAllUsers() {
        List<UserInfoDTO> users = new ArrayList<>();
        users.add(toUserInfo(getUserDetails(null)));
        users.addAll(pull(Query.otherUsers()));
        return users;
    }

    @Override
    public UserDetailsDTO getUserDetails(String userName) {
        return pull(Query.account());
    }

    @Override
    public MarketStateDTO getMarketState(int eventId) {
        return pull(Query.market(eventId));
    }

    @Override
    public OrderBookStateDTO getOrderBookState(int eventId) {
        return pull(Query.orderBook(eventId));
    }

    @Override
    public List<PriceHistoryDTO> getEventPriceHistory(int eventId) {
        return pull(Query.prices(eventId));
    }

    @Override
    public List<HistoryPointDTO> getUserBalanceHistory(String userName) {
        return pull(Query.balanceHistory());
    }

    /**
     * Logs in under the given name; the server keeps the name in the session of this client.
     */
    @Override
    public UserInfoDTO registerUser(String userName) {
        api.post("/login", Map.of("username", userName), null);
        return toUserInfo(getUserDetails(userName));
    }

    @Override
    public UserInfoDTO deposit(String userName, double amount) {
        return gson.fromJson(api.post("/account/deposit", Map.of("amount", String.valueOf(amount)), null),
                UserInfoDTO.class);
    }

    @Override
    public UploadResultDTO uploadEvents(String userName, String fileName, InputStream content) {
        try {
            return gson.fromJson(api.upload("/upload", fileName, content.readAllBytes()), UploadResultDTO.class);
        } catch (IOException e) {
            throw new ServerException("The file could not be read: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AccountEntryDTO> getAccountEntries(String userName) {
        return pull(Query.accountEntries());
    }

    @Override
    public EventInfoDTO createEvent(NewEventRequestDTO request) {
        return gson.fromJson(api.post("/event/create", NO_PARAMS, gson.toJson(request)), EventInfoDTO.class);
    }

    @Override
    public EventInfoDTO openEvent(int eventId, String userName) {
        return gson.fromJson(api.post("/event/open", Query.eventParam(eventId), null), EventInfoDTO.class);
    }

    @Override
    public PurchaseResultDTO buyShares(int eventId, String userName, int optionIndex, long quantity) {
        String body = gson.toJson(new BuyBody(eventId, optionIndex, quantity));
        return gson.fromJson(api.post("/event/buy", NO_PARAMS, body), PurchaseResultDTO.class);
    }

    @Override
    public OrderResultDTO submitOrder(OrderRequestDTO request) {
        return gson.fromJson(api.post("/event/order", NO_PARAMS, gson.toJson(request)), OrderResultDTO.class);
    }

    @Override
    public EventInfoDTO closeEvent(int eventId, String userName, int winningOptionIndex) {
        Map<String, String> params = Map.of("id", String.valueOf(eventId),
                "winner", String.valueOf(winningOptionIndex));
        return gson.fromJson(api.post("/event/close", params, null), EventInfoDTO.class);
    }

    /**
     * Sends a read request and waits for its answer.
     */
    private <T> T pull(Query<T> query) {
        return query.reader().apply(api.get(query.path(), query.params()));
    }

    private static UserInfoDTO toUserInfo(UserDetailsDTO details) {
        boolean isMarketMaker = details.events().stream().anyMatch(UserEventDTO::marketMaker);
        return new UserInfoDTO(details.name(), details.balance(), details.blocked(), isMarketMaker);
    }
}
