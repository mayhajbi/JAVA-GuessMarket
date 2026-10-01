package gm.ui.fx.eventdetail;

import gm.engine.api.GuessMarketEngine;
import javafx.fxml.FXML;

/**
 * A screen that shows the details of an event next to its own content: the events screen and the
 * account screen. It connects the included details component to the engine and to the user who is
 * logged in, the same way for both.
 */
public abstract class EventDetailScreen {

    @FXML protected EventDetailController eventDetailComponentController;

    protected GuessMarketEngine engine;
    /** The name of the user who is logged in. */
    protected String userName;

    /**
     * @param onDataChanged called after an action changed the data in the engine
     */
    public void setOnDataChanged(Runnable onDataChanged) {
        eventDetailComponentController.setOnDataChanged(onDataChanged);
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventDetailComponentController.setEngine(engine);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}