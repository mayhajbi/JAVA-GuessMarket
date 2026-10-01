package gm.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gm.dto.AccountEntryDTO;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.HistoryPointDTO;
import gm.dto.LoadResultDTO;
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
import gm.dto.UserSummaryDTO;
import gm.engine.api.GuessMarketEngine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The engine of the client: every method is a request to the server and the answer is turned back into the
 * DTO the screens of exercise 2 already work with. The user who acts is always the one who logged in -
 * the server knows it from the session - so the user names in the signatures are not sent.
 */
public class HttpGuessMarketEngine implements GuessMarketEngine {

    private static final Type EVENTS = new TypeToken<List<EventInfoDTO>>() { }.getType();
    private static final Type USERS = new TypeToken<List<UserSummaryDTO>>() { }.getType();
    private static final Type PRICE_HISTORIES = new TypeToken<List<PriceHistoryDTO>>() { }.getType();
    private static final Type ACCOUNT_ENTRIES = new TypeToken<List<AccountEntryDTO>>() { }.getType();
    private static final Type BALANCE_HISTORY = new TypeToken<List<HistoryPointDTO>>() { }.getType();
    private static final Map<String, String> NO_PARAMS = Map.of();

    private record BuyBody(int eventId, int optionIndex, long quantity) {
    }

    private final HttpApi api = new HttpApi();
    private final Gson gson = new Gson();

    @Override
    public LoadResultDTO loadEventsFile(String xmlFilePath) {
        throw new UnsupportedOperationException("Loading a file from a path is not available in the client.");
    }

    @Override
    public List<EventInfoDTO> getAllEvents() {
        return gson.fromJson(api.get("/events", NO_PARAMS), EVENTS);
    }

    @Override
    public List<EventInfoDTO> getEvents(EventFilterDTO filter) {
        Map<String, String> params = Map.of(
                "types", names(filter.types()),
                "statuses", names(filter.statuses()),
                "commissions", names(filter.commissionTypes()));
        return gson.fromJson(api.get("/events", params), EVENTS);
    }

    /**
     * The user who is logged in comes first, followed by the other users, of whom the server tells only the
     * name, the balance and whether they are a market maker.
     */
    @Override
    public List<UserInfoDTO> getAllUsers() {
        List<UserInfoDTO> users = new ArrayList<>();
        users.add(toUserInfo(getUserDetails(null)));
        List<UserSummaryDTO> others = gson.fromJson(api.get("/users", NO_PARAMS), USERS);
        for (UserSummaryDTO other : others) {
            users.add(new UserInfoDTO(other.name(), other.balance(), false, other.marketMaker()));
        }
        return users;
    }

    @Override
    public UserDetailsDTO getUserDetails(String userName) {
        return gson.fromJson(api.get("/account", NO_PARAMS), UserDetailsDTO.class);
    }

    @Override
    public MarketStateDTO getMarketState(int eventId) {
        return gson.fromJson(api.get("/event/market", eventParam(eventId)), MarketStateDTO.class);
    }

    @Override
    public OrderBookStateDTO getOrderBookState(int eventId) {
        return gson.fromJson(api.get("/event/orderbook", eventParam(eventId)), OrderBookStateDTO.class);
    }

    @Override
    public List<PriceHistoryDTO> getEventPriceHistory(int eventId) {
        return gson.fromJson(api.get("/event/prices", eventParam(eventId)), PRICE_HISTORIES);
    }

    @Override
    public List<HistoryPointDTO> getUserBalanceHistory(String userName) {
        return gson.fromJson(api.get("/account/history", NO_PARAMS), BALANCE_HISTORY);
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
        return gson.fromJson(api.get("/account/log", NO_PARAMS), ACCOUNT_ENTRIES);
    }

    @Override
    public EventInfoDTO createEvent(NewEventRequestDTO request) {
        return gson.fromJson(api.post("/event/create", NO_PARAMS, gson.toJson(request)), EventInfoDTO.class);
    }

    @Override
    public EventInfoDTO openEvent(int eventId, String userName) {
        return gson.fromJson(api.post("/event/open", eventParam(eventId), null), EventInfoDTO.class);
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

    private static Map<String, String> eventParam(int eventId) {
        return Map.of("id", String.valueOf(eventId));
    }

    /**
     * The names of the chosen values, comma separated, as the server expects a filter.
     */
    private static String names(Iterable<? extends Enum<?>> values) {
        List<String> names = new ArrayList<>();
        values.forEach(value -> names.add(value.name()));
        return names.stream().collect(Collectors.joining(","));
    }

    private static UserInfoDTO toUserInfo(UserDetailsDTO details) {
        boolean isMarketMaker = details.events().stream().anyMatch(UserEventDTO::marketMaker);
        return new UserInfoDTO(details.name(), details.balance(), details.blocked(), isMarketMaker);
    }
}
