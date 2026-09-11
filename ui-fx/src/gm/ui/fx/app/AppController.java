package gm.ui.fx.app;

import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.events.EventsController;
import gm.ui.fx.header.HeaderController;
import gm.ui.fx.users.UsersController;
import javafx.fxml.FXML;

/**
 * The main controller of the application. It connects the header and the two screens (events and
 * users) and hands them the engine.
 */
public class AppController {

    @FXML private HeaderController headerComponentController;
    @FXML private EventsController eventsComponentController;
    @FXML private UsersController usersComponentController;

    @FXML
    private void initialize() {
        headerComponentController.setMainController(this);
    }

    public void setEngine(GuessMarketEngine engine) {
        headerComponentController.setEngine(engine);
        eventsComponentController.setEngine(engine);
        usersComponentController.setEngine(engine);
    }

    /**
     * Called after a data file was loaded successfully: every screen pulls the new data from the
     * engine.
     */
    public void onSystemLoaded() {
        eventsComponentController.refresh();
        usersComponentController.refresh();
    }
}
