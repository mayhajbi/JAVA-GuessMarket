package gm.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gm.dto.AccountEntryDTO;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStateDTO;
import gm.dto.EventType;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserInfoDTO;
import gm.dto.UserSummaryDTO;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One request that reads data from the server: where it is sent and how its answer is read. Every read
 * request of the client is described here once, and is used both by the engine of the client, which waits
 * for the answer, and by a {@link Refresher}, which does not.
 *
 * @param path   the part of the address after the base, for example {@code /events}
 * @param params the query parameters; may be empty
 * @param reader turns the body of the answer into the data
 * @param <T>    the data the request answers with
 */
public record Query<T>(String path, Map<String, String> params, Function<String, T> reader) {

    private static final Gson GSON = new Gson();
    private static final Map<String, String> NO_PARAMS = Map.of();
    private static final Type EVENTS = new TypeToken<List<EventInfoDTO>>() { }.getType();
    private static final Type USERS = new TypeToken<List<UserSummaryDTO>>() { }.getType();
    private static final Type PRICE_HISTORIES = new TypeToken<List<PriceHistoryDTO>>() { }.getType();
    private static final Type ACCOUNT_ENTRIES = new TypeToken<List<AccountEntryDTO>>() { }.getType();
    private static final Type BALANCE_HISTORY = new TypeToken<List<HistoryPointDTO>>() { }.getType();

    public static Query<List<EventInfoDTO>> allEvents() {
        return json("/events", NO_PARAMS, EVENTS);
    }

    public static Query<List<EventInfoDTO>> events(EventFilterDTO filter) {
        return json("/events", Map.of(
                "types", names(filter.types()),
                "statuses", names(filter.statuses()),
                "commissions", names(filter.commissionTypes())), EVENTS);
    }

    /**
     * The details of the user who is logged in; the server knows who that is from the session.
     */
    public static Query<UserDetailsDTO> account() {
        return json("/account", NO_PARAMS, UserDetailsDTO.class);
    }

    /**
     * The users other than the one who is logged in. The server tells only their name, their balance and
     * whether they are a market maker.
     */
    public static Query<List<UserInfoDTO>> otherUsers() {
        return new Query<>("/users", NO_PARAMS, body -> {
            List<UserSummaryDTO> others = GSON.fromJson(body, USERS);
            return others.stream()
                    .map(other -> new UserInfoDTO(other.name(), other.balance(), false, other.marketMaker()))
                    .toList();
        });
    }

    public static Query<List<AccountEntryDTO>> accountEntries() {
        return json("/account/log", NO_PARAMS, ACCOUNT_ENTRIES);
    }

    public static Query<List<HistoryPointDTO>> balanceHistory() {
        return json("/account/history", NO_PARAMS, BALANCE_HISTORY);
    }

    public static Query<MarketStateDTO> market(int eventId) {
        return json("/event/market", eventParam(eventId), MarketStateDTO.class);
    }

    public static Query<OrderBookStateDTO> orderBook(int eventId) {
        return json("/event/orderbook", eventParam(eventId), OrderBookStateDTO.class);
    }

    /**
     * The state of an event, asked the way its trading method needs.
     */
    public static Query<? extends EventStateDTO> state(EventInfoDTO event) {
        return event.type() == EventType.LMSR ? market(event.id()) : orderBook(event.id());
    }

    public static Query<List<PriceHistoryDTO>> prices(int eventId) {
        return json("/event/prices", eventParam(eventId), PRICE_HISTORIES);
    }

    /**
     * @return whether both requests ask the server the same thing
     */
    public boolean asksTheSameAs(Query<?> other) {
        return other != null && path.equals(other.path) && params.equals(other.params);
    }

    /**
     * A request whose answer is the JSON of the given type.
     */
    private static <T> Query<T> json(String path, Map<String, String> params, Type type) {
        return new Query<>(path, params, body -> GSON.fromJson(body, type));
    }

    static Map<String, String> eventParam(int eventId) {
        return Map.of("id", String.valueOf(eventId));
    }

    /**
     * The names of the chosen values, comma separated, as the server expects a filter.
     */
    private static String names(Iterable<? extends Enum<?>> values) {
        List<String> names = new ArrayList<>();
        values.forEach(value -> names.add(value.name()));
        return String.join(",", names);
    }
}
