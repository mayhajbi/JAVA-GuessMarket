import gm.client.Constants;
import gm.client.HttpApi;
import gm.client.HttpGuessMarketEngine;
import gm.client.Query;
import gm.client.Refresher;
import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.UserDetailsDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The automatic updates of the client against the running server, with the same classes and the same
 * rate the client uses: a client that is not logged in is told so, a logged in client gets its data,
 * what another user does reaches it within the 2 seconds the exercise allows, and nothing is pulled
 * while the updates are turned off or there is nothing to ask.
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
        } finally {
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
