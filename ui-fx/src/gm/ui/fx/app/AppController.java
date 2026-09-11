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
        eventsComponentController.setMainController(this);
        usersComponentController.setMainController(this);
    }

    public void setEngine(GuessMarketEngine engine) {
        headerComponentController.setEngine(engine);
        eventsComponentController.setEngine(engine);
        usersComponentController.setEngine(engine);
    }

    /**
     * Called whenever the data in the engine changed - a file was loaded or an action was performed.
     * The engine never pushes updates, so every screen pulls the current data again.
     */
    public void refreshAll() {
        eventsComponentController.refresh();
        usersComponentController.refresh();
    }
}
