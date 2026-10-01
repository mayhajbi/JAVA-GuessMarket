package gm.client.app;

import gm.client.Constants;
import gm.client.HttpApi;
import gm.client.Query;
import gm.client.Refresher;
import gm.client.account.AccountController;
import gm.client.header.HeaderController;
import gm.dto.EventInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailController;
import gm.ui.fx.events.EventsController;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * The main controller of the client. It connects the header and the two screens (events and account),
 * hands them the engine and the name of the user who is logged in, and keeps them up to date with what
 * the other users do.
 */
public class AppController {

    /** The status code of the server for a request of a user who is not logged in. */
    private static final int UNAUTHORIZED = 401;

    @FXML private HeaderController headerComponentController;
    @FXML private EventsController eventsComponentController;
    @FXML private AccountController accountComponentController;
    @FXML private Tab eventsTab;
    @FXML private Tab accountTab;

    @FXML private TabPane screens;
    @FXML private Label serverStatusLabel;

    /** One timer for every part of the screen that refreshes itself. */
    private final List<Timer> timers = new ArrayList<>();
    /** How many refresh requests in a row got no answer from the server. */
    private final AtomicInteger failures = new AtomicInteger();
    private HttpApi api;
    private Runnable onLoggedOut;

    @FXML
    private void initialize() {
        ViewUtils.show(serverStatusLabel, false);
    }

    /**
     * Starts the screens for the user who logged in.
     *
     * @param api         the connection to the server, for the automatic updates
     * @param onLoggedOut called when the server does not know the user any more
     */
    public void start(GuessMarketEngine engine, HttpApi api, String userName, Runnable onLoggedOut) {
        this.api = api;
        this.onLoggedOut = onLoggedOut;
        eventsComponentController.setEngine(engine);
        eventsComponentController.setUserName(userName);
        eventsComponentController.setOnDataChanged(this::refreshAll);
        accountComponentController.setEngine(engine);
        accountComponentController.setUserName(userName);
        accountComponentController.setOnDataChanged(this::refreshAll);
        refreshAll();
        startRefreshers();
    }

    /**
     * Called whenever the user who is logged in performed an action, so that its result is seen at once,
     * without waiting for the next automatic update.
     */
    public void refreshAll() {
        eventsComponentController.refresh();
        accountComponentController.refresh();
    }

    /**
     * Stops the automatic updates, when the application exits.
     */
    public void close() {
        timers.forEach(Timer::cancel);
        timers.clear();
    }

    /**
     * Starts the automatic updates: what the other users do reaches this client by pulling the data again
     * at a fixed rate. A screen is refreshed only while its tab is the one shown.
     */
    private void startRefreshers() {
        ObservableBooleanValue eventsShown = eventsTab.selectedProperty();
        Timer eventsTimer = newTimer();
        schedule(eventsTimer, eventsShown, Query::account, eventsComponentController::showActingUser,
                this::onAnswer);
        schedule(eventsTimer, eventsShown, Query::allEvents, eventsComponentController::showAllEvents);
        schedule(eventsTimer, eventsShown, () -> Query.events(eventsComponentController.filter()),
                eventsComponentController::showEvents);
        scheduleEventDetail(eventsShown, eventsComponentController.eventDetail());

        ObservableBooleanValue accountShown = accountTab.selectedProperty();
        Timer accountTimer = newTimer();
        schedule(accountTimer, accountShown, Query::account, accountComponentController::showAccount,
                this::onAnswer);
        schedule(accountTimer, accountShown, Query::otherUsers, accountComponentController::showOtherUsers);
        schedule(accountTimer, accountShown, Query::accountEntries,
                accountComponentController::showAccountEntries);
        schedule(accountTimer, accountShown, Query::balanceHistory,
                accountComponentController::showBalanceHistory);
        scheduleEventDetail(accountShown, accountComponentController.eventDetail());
    }

    /**
     * Refreshes the details of the event a details component shows, while it shows one.
     */
    private void scheduleEventDetail(ObservableBooleanValue shouldUpdate, EventDetailController eventDetail) {
        Timer timer = newTimer();
        schedule(timer, shouldUpdate, () -> {
            EventInfoDTO event = eventDetail.shownEvent();
            return event == null ? null : Query.state(event);
        }, eventDetail::showState);
        schedule(timer, shouldUpdate, () -> {
            EventInfoDTO event = eventDetail.shownEvent();
            return event == null ? null : Query.prices(event.id());
        }, eventDetail::showPrices);
    }

    /**
     * Runs a refresher on the timer at the fixed rate, the way the course example does. The data it pulls
     * is shown on the JavaFX thread.
     *
     * @param query the request to send, asked again before every request; {@code null} when there is
     *              nothing to pull right now
     * @param show  shows the data on the screen
     */
    private <T> void schedule(Timer timer, ObservableBooleanValue shouldUpdate, Supplier<Query<? extends T>> query,
                              Consumer<T> show) {
        schedule(timer, shouldUpdate, query, show, status -> { });
    }

    /**
     * @param statusConsumer receives the status code of every answer, or {@link Refresher#NO_ANSWER}
     */
    private <T> void schedule(Timer timer, ObservableBooleanValue shouldUpdate, Supplier<Query<? extends T>> query,
                              Consumer<T> show, IntConsumer statusConsumer) {
        Refresher<T> refresher = new Refresher<>(api, shouldUpdate, query,
                (asked, data) -> Platform.runLater(() -> {
                    // An answer to what is not asked any more - another event was selected or the filter
                    // was changed while the request was on its way - is dropped.
                    if (asked.asksTheSameAs(query.get())) {
                        show.accept(data);
                    }
                }),
                statusConsumer);
        timer.schedule(refresher, Constants.REFRESH_RATE, Constants.REFRESH_RATE);
    }

    /**
     * Follows the answers of the server to the refresh of the account, one in every cycle. After a few
     * cycles in a row without an answer the server is reported as not reachable and the screens are
     * locked, until it answers again. A server that does not know the user any more was restarted, so the
     * user is sent back to the login screen.
     * <p>
     * Called on a thread of the HTTP client.
     */
    private void onAnswer(int status) {
        if (status == Refresher.NO_ANSWER) {
            if (failures.incrementAndGet() == Constants.MAX_FAILURES) {
                Platform.runLater(() -> showServerReachable(false));
            }
            return;
        }
        if (failures.getAndSet(0) >= Constants.MAX_FAILURES) {
            Platform.runLater(() -> showServerReachable(true));
        }
        if (status == UNAUTHORIZED) {
            Platform.runLater(this::logOut);
        }
    }

    private void showServerReachable(boolean reachable) {
        ViewUtils.show(serverStatusLabel, !reachable);
        screens.setDisable(!reachable);
    }

    private void logOut() {
        // Requests that were already on their way may report the same thing again.
        if (timers.isEmpty()) {
            return;
        }
        close();
        onLoggedOut.run();
    }

    private Timer newTimer() {
        Timer timer = new Timer();
        timers.add(timer);
        return timer;
    }
}
