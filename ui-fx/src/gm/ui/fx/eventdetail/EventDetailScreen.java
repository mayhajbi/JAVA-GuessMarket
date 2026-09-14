package gm.ui.fx.eventdetail;

import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.app.AppController;
import javafx.fxml.FXML;

/**
 * A screen that shows the details of an event next to its own content: the events screen and the
 * users screen. It connects the included details component to the engine and to the main controller,
 * the same way for both.
 */
public abstract class EventDetailScreen {

    @FXML protected EventDetailController eventDetailComponentController;

    protected GuessMarketEngine engine;

    public void setMainController(AppController mainController) {
        eventDetailComponentController.setOnDataChanged(mainController::refreshAll);
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventDetailComponentController.setEngine(engine);
    }
}
