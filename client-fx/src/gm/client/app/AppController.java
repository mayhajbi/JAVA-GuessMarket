package gm.client.app;

import gm.client.account.AccountController;
import gm.client.header.HeaderController;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.events.EventsController;
import javafx.fxml.FXML;

/**
 * The main controller of the client. It connects the header and the two screens (events and account)
 * and hands them the engine and the name of the user who is logged in.
 */
public class AppController {

    @FXML private HeaderController headerComponentController;
    @FXML private EventsController eventsComponentController;
    @FXML private AccountController accountComponentController;

    /**
     * Starts the screens for the user who logged in.
     */
    public void start(GuessMarketEngine engine, String userName) {
        eventsComponentController.setEngine(engine);
        eventsComponentController.setUserName(userName);
        eventsComponentController.setOnDataChanged(this::refreshAll);
        accountComponentController.setEngine(engine);
        accountComponentController.setUserName(userName);
        accountComponentController.setOnDataChanged(this::refreshAll);
        refreshAll();
    }

    /**
     * Called whenever the data in the engine changed - an action was performed. The engine never pushes
     * updates, so every screen pulls the current data again.
     */
    public void refreshAll() {
        eventsComponentController.refresh();
        accountComponentController.refresh();
    }
}