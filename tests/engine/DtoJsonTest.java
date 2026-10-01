import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gm.dto.AccountEntryDTO;
import gm.dto.ChatLinesDTO;
import gm.dto.EventInfoDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderSide;
import gm.dto.PriceHistoryDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserSummaryDTO;
import gm.engine.impl.GuessMarketEngineImpl;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Every DTO the server answers with survives the way to the client: it becomes JSON with gson and
 * comes back as an object equal to the original. The DTOs are taken from an engine with events that
 * are open, traded in and closed, so that they hold real numbers, empty values and lists.
 * The engine starts as {@link Scenario#multiple()}.
 */
public class DtoJsonTest extends Check {

    static final Gson GSON = new Gson();

    public static void main(String[] args) {
        run("dto-json", DtoJsonTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = Scenario.multiple();

        // Before anything happened: events that are not open, and lists that are still empty.
        roundTrip(engine.getAllEvents(), new TypeToken<List<EventInfoDTO>>() { }.getType(), "events at load");
        roundTrip(engine.getEvents(new gm.dto.EventFilterDTO(java.util.Set.of(), java.util.Set.of(),
                java.util.Set.of())), new TypeToken<List<EventInfoDTO>>() { }.getType(), "no events");
        roundTrip(engine.getMarketState(1), MarketStateDTO.class, "an LMSR state before it opens");
        roundTrip(engine.getEventPriceHistory(1), new TypeToken<List<PriceHistoryDTO>>() { }.getType(),
                "prices before it opens");

        // While they are traded in.
        engine.openEvent(1, "Tikva");
        engine.buyShares(1, "Menash", 0, 10);
        engine.openEvent(2, "Avrum");
        engine.submitOrder(new OrderRequestDTO(2, "Menash", OrderSide.BUY, 0, 5, 0.40));
        engine.submitOrder(new OrderRequestDTO(2, "Avrum", OrderSide.SELL, 0, 5, 0.60));
        roundTrip(engine.getMarketState(1), MarketStateDTO.class, "an LMSR state while active");
        roundTrip(engine.getOrderBookState(2), OrderBookStateDTO.class, "an order book state while active");
        roundTrip(engine.getEventPriceHistory(1), new TypeToken<List<PriceHistoryDTO>>() { }.getType(),
                "LMSR prices");
        roundTrip(engine.getEventPriceHistory(2), new TypeToken<List<PriceHistoryDTO>>() { }.getType(),
                "order book prices (options that were never quoted)");

        // After they are closed: a winning option instead of none.
        engine.closeEvent(1, "Tikva", 0);
        engine.closeEvent(2, "Avrum", 1);
        roundTrip(engine.getMarketState(1), MarketStateDTO.class, "a closed LMSR state");
        roundTrip(engine.getOrderBookState(2), OrderBookStateDTO.class, "a closed order book state");
        roundTrip(engine.getAllEvents(), new TypeToken<List<EventInfoDTO>>() { }.getType(), "events at the end");

        // The users and the account.
        roundTrip(engine.getAllUsers().stream()
                .map(user -> new UserSummaryDTO(user.name(), user.balance(), user.marketMaker())).toList(),
                new TypeToken<List<UserSummaryDTO>>() { }.getType(), "the other users");
        roundTrip(engine.getUserDetails("Menash"), UserDetailsDTO.class, "the details of a user");
        roundTrip(engine.getAccountEntries("Menash"), new TypeToken<List<AccountEntryDTO>>() { }.getType(),
                "the account entries");
        roundTrip(engine.getAccountEntries("Tikva"), new TypeToken<List<AccountEntryDTO>>() { }.getType(),
                "the account entries of a market maker");

        // The chat (bonus): no lines at all, and then lines.
        roundTrip(engine.getChatLines(0), ChatLinesDTO.class, "an empty chat");
        engine.sendChatLine("Menash", "Hello");
        engine.sendChatLine("Tikva", "Hi Menash");
        roundTrip(engine.getChatLines(0), ChatLinesDTO.class, "a chat with lines");
    }

    static void roundTrip(Object value, Type type, String what) {
        Object back = GSON.fromJson(GSON.toJson(value), type);
        expect(value, back, what);
    }
}
