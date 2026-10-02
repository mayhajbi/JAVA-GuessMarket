import gm.client.Constants;
import gm.client.HttpApi;
import gm.client.HttpGuessMarketEngine;
import gm.client.Query;
import gm.client.Refresher;
import gm.dto.AccountEntryDTO;
import gm.dto.AccountEntryType;
import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.MarketStateDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderSide;
import gm.dto.UserDetailsDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import okhttp3.OkHttpClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The automatic updates of the client against the running server, with the same classes and the same
 * rate the client uses: a client that is not logged in is told so, a logged in client gets its data,
 * what another user does - logging in, loading funds, uploading, opening, trading and closing - reaches
 * it within the 2 seconds the exercise allows, nothing is pulled while the updates are turned off or there
 * is nothing to ask, and no connection is leaked on the way.
 */
public class LivePullCheck extends Check {

    /** The longest the exercise lets an update take. */
    static final long UPDATE_LIMIT_MILLIS = 2000;
    static final int UNAUTHORIZED = 401;
    static final int OK = 200;

    /** What a refresher brought so far. */
    static final class Pulled<T> {
        volatile T data;
        volatile int status = -1;
        final AtomicInteger answers = new AtomicInteger();
    }

    public static void main(String[] args) {
        run("live-pull", LivePullCheck::check);
    }

    static void check() throws Exception {
        String suffix = String.valueOf(System.nanoTime() % 1_000_000);
        String firstName = "pull_a_" + suffix;
        String secondName = "pull_b_" + suffix;
        HttpApi firstApi = new HttpApi();
        HttpApi secondApi = new HttpApi();
        GuessMarketEngine first = new HttpGuessMarketEngine(firstApi);
        GuessMarketEngine second = new HttpGuessMarketEngine(secondApi);
        Timer timer = new Timer(true);
        BooleanProperty on = new SimpleBooleanProperty(true);
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());
        Logger httpLog = Logger.getLogger(OkHttpClient.class.getName());
        Handler watcher = warningsInto(warnings);
        httpLog.addHandler(watcher);
        try {
            Pulled<UserDetailsDTO> account = start(timer, firstApi, on, Query::account);
            expectTrue(waitFor(() -> account.status == UNAUTHORIZED), "a client that is not logged in is told so");
            expect(null, account.data, "a client that is not logged in gets no data");

            first.registerUser(firstName);
            expectTrue(waitFor(() -> account.data != null), "a logged in client gets its account");
            expect(firstName, account.data == null ? null : account.data.name(), "the account is the one of the session");
            expectTrue(waitFor(() -> account.status == OK), "a logged in client is answered with 200");

            Pulled<List<UserInfoDTO>> others = start(timer, firstApi, on, Query::otherUsers);
            second.registerUser(secondName);
            expectTrue(waitFor(() -> balanceOf(others, secondName) != null), "a user who logged in reaches the other client");

            long started = System.currentTimeMillis();
            second.deposit(secondName, 123.45);
            boolean arrived = waitFor(() -> Double.valueOf(123.45).equals(balanceOf(others, secondName)));
            long took = System.currentTimeMillis() - started;
            expectTrue(arrived && took < UPDATE_LIMIT_MILLIS,
                    "what another user does reaches the client within " + UPDATE_LIMIT_MILLIS + "ms (took " + took + "ms)");

            Pulled<UserDetailsDTO> nothingToAsk = start(timer, firstApi, on, () -> null);
            on.set(false);
            // A request that was already on its way may still be answered.
            Thread.sleep(Constants.REFRESH_RATE);
            int answersWhenTurnedOff = account.answers.get();
            Thread.sleep(3L * Constants.REFRESH_RATE);
            expect(answersWhenTurnedOff, account.answers.get(), "nothing is pulled while the updates are turned off");
            on.set(true);
            expectTrue(waitFor(() -> account.answers.get() > answersWhenTurnedOff), "the updates continue once turned on");
            expect(0, nothingToAsk.answers.get(), "nothing is pulled while there is nothing to ask");

            sameQuestion();
            trading(timer, on, firstApi, secondApi, first, second, firstName, secondName, suffix);
            noLeak(warnings, account.answers.get());
        } finally {
            httpLog.removeHandler(watcher);
            timer.cancel();
            firstApi.shutdown();
            secondApi.shutdown();
        }
    }

    static void sameQuestion() {
        expectTrue(Query.prices(1).asksTheSameAs(Query.prices(1)), "the prices of the same event are the same question");
        expectFalse(Query.prices(1).asksTheSameAs(Query.prices(2)), "the prices of another event are another question");
        expectFalse(Query.prices(1).asksTheSameAs(Query.market(1)), "the state of the event is another question");
        expectFalse(Query.prices(1).asksTheSameAs(null), "nothing to ask is another question");
        EventFilterDTO everything = new EventFilterDTO(EnumSet.allOf(EventType.class),
                EnumSet.allOf(EventStatus.class), EnumSet.allOf(CommissionType.class));
        EventFilterDTO lmsrOnly = new EventFilterDTO(EnumSet.of(EventType.LMSR),
                EnumSet.allOf(EventStatus.class), EnumSet.allOf(CommissionType.class));
        expectTrue(Query.events(everything).asksTheSameAs(Query.events(everything)), "the same filter is the same question");
        expectFalse(Query.events(everything).asksTheSameAs(Query.events(lmsrOnly)), "another filter is another question");
    }

    /**
     * What one user does in the events reaches the other one within the time the exercise allows: new
     * events, an opened event, a purchase, an order book trade, the commission of the market maker, and
     * the closing of an event with its payout.
     */
    static void trading(Timer timer, ObservableBooleanValue on, HttpApi makerApi, HttpApi traderApi,
                        GuessMarketEngine maker, GuessMarketEngine trader, String makerName, String traderName,
                        String suffix) throws Exception {
        String lmsrName = "Pull LMSR " + suffix;
        String bookName = "Pull Book " + suffix;
        Pulled<List<EventInfoDTO>> traderEvents = start(timer, traderApi, on, Query::allEvents);
        Pulled<List<AccountEntryDTO>> makerRows = start(timer, makerApi, on, Query::accountEntries);
        Pulled<List<AccountEntryDTO>> traderRows = start(timer, traderApi, on, Query::accountEntries);

        String file = "<Guess-Market><GM-events>" + event(lmsrName, "<GM-LMSR><b>10</b></GM-LMSR>")
                + event(bookName, "<GM-order-book allow-mint=\"false\" initial=\"10\" d=\"1\"/>")
                + "</GM-events></Guess-Market>";
        maker.uploadEvents(makerName, "pull.xml", new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));
        expectTrue(waitFor(() -> statusOf(traderEvents, lmsrName) == EventStatus.INACTIVE
                && statusOf(traderEvents, bookName) == EventStatus.INACTIVE), "uploaded events reach the other client");
        int lmsrId = idOf(maker, lmsrName);
        int bookId = idOf(maker, bookName);

        maker.deposit(makerName, 1000);
        maker.openEvent(lmsrId, makerName);
        maker.openEvent(bookId, makerName);
        expectTrue(waitFor(() -> statusOf(traderEvents, lmsrName) == EventStatus.ACTIVE
                && statusOf(traderEvents, bookName) == EventStatus.ACTIVE), "opened events reach the other client");

        Pulled<MarketStateDTO> makerMarket = start(timer, makerApi, on, () -> Query.market(lmsrId));
        trader.buyShares(lmsrId, traderName, 0, 5);
        expectTrue(waitFor(() -> makerMarket.data != null && makerMarket.data.tradeHistory().size() == 1),
                "a purchase reaches the client of the market maker");
        expectTrue(waitFor(() -> count(makerRows, AccountEntryType.COMMISSION) == 1),
                "the commission of a purchase appears in the account of the market maker");

        Pulled<OrderBookStateDTO> makerBook = start(timer, makerApi, on, () -> Query.orderBook(bookId));
        trader.submitOrder(new OrderRequestDTO(bookId, traderName, OrderSide.BUY, 0, 5, 0.40));
        expectTrue(waitFor(() -> makerBook.data != null && makerBook.data.participants().size() == 2),
                "a waiting order reaches the client of the market maker");
        expect(1L, count(makerRows, AccountEntryType.COMMISSION), "a waiting order pays no commission");
        maker.submitOrder(new OrderRequestDTO(bookId, makerName, OrderSide.SELL, 0, 5, 0.40));
        expectTrue(waitFor(() -> count(makerRows, AccountEntryType.COMMISSION) == 2),
                "the commission of an order book trade appears in the account of the market maker");

        maker.closeEvent(lmsrId, makerName, 0);
        maker.closeEvent(bookId, makerName, 0);
        expectTrue(waitFor(() -> statusOf(traderEvents, lmsrName) == EventStatus.CLOSED
                && statusOf(traderEvents, bookName) == EventStatus.CLOSED), "closed events reach the other client");
        expectTrue(waitFor(() -> count(traderRows, AccountEntryType.PAYOUT) == 2),
                "the payout of both events appears in the account of the winner");
    }

    /** One event of a file, with a commission of 10% on purchase and the given trading method. */
    static String event(String name, String method) {
        return "<GM-event name=\"" + name + "\"><description>Checks the automatic updates</description>"
                + "<commission type=\"on-purchase\">10</commission><GM-options><GM-option>Yes</GM-option>"
                + "<GM-option>No</GM-option></GM-options><GM-method>" + method + "</GM-method></GM-event>";
    }

    static EventStatus statusOf(Pulled<List<EventInfoDTO>> events, String name) {
        List<EventInfoDTO> pulled = events.data;
        if (pulled == null) {
            return null;
        }
        return pulled.stream().filter(event -> event.name().equals(name)).map(EventInfoDTO::status)
                .findFirst().orElse(null);
    }

    static int idOf(GuessMarketEngine engine, String eventName) {
        return engine.getAllEvents().stream().filter(event -> event.name().equals(eventName))
                .findFirst().orElseThrow().id();
    }

    static long count(Pulled<List<AccountEntryDTO>> rows, AccountEntryType type) {
        List<AccountEntryDTO> pulled = rows.data;
        return pulled == null ? 0 : pulled.stream().filter(row -> row.type() == type).count();
    }

    /**
     * A response that was not closed keeps its connection. The HTTP client finds such a connection once
     * the garbage collector ran, while it handles further requests, and warns about it in its log.
     */
    static void noLeak(List<String> warnings, int requests) throws InterruptedException {
        for (int round = 0; round < 3; round++) {
            System.gc();
            Thread.sleep(2L * Constants.REFRESH_RATE);
        }
        expect("[]", warnings.toString(),
                "no connection was leaked (the account alone was pulled " + requests + " times)");
    }

    /** Collects what the HTTP client warns about in its log. */
    static Handler warningsInto(List<String> warnings) {
        return new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }

    /** Runs a refresher on the timer at the rate of the client, and keeps what it brings. */
    static <T> Pulled<T> start(Timer timer, HttpApi api, ObservableBooleanValue shouldUpdate,
                               Supplier<Query<? extends T>> query) {
        Pulled<T> pulled = new Pulled<>();
        timer.schedule(new Refresher<T>(api, shouldUpdate, query, (asked, data) -> pulled.data = data, status -> {
            pulled.status = status;
            pulled.answers.incrementAndGet();
        }), Constants.REFRESH_RATE, Constants.REFRESH_RATE);
        return pulled;
    }

    static Double balanceOf(Pulled<List<UserInfoDTO>> others, String name) {
        List<UserInfoDTO> users = others.data;
        if (users == null) {
            return null;
        }
        return users.stream().filter(user -> user.name().equals(name)).map(UserInfoDTO::balance).findFirst().orElse(null);
    }

    /** Waits until the condition holds, for the longest the exercise lets an update take. */
    static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + UPDATE_LIMIT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }
}
