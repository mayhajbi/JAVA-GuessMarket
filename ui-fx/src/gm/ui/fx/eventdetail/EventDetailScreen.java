package gm.ui.fx.eventdetail;

import gm.dto.EventInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Function;

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
    /** Whether the rows of the table of events are being replaced right now. */
    private boolean replacingRows;

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

    /**
     * @return the details component of this screen, to connect it to the automatic updates
     */
    public EventDetailController eventDetail() {
        return eventDetailComponentController;
    }

    /**
     * Makes the details component show the event of the row the user selects in the table.
     *
     * @param eventOf the event a row of the table describes
     */
    protected <S> void showSelectedEvent(TableView<S> table, Function<S, EventInfoDTO> eventOf) {
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (!replacingRows) {
                showEventOf(selected, eventOf);
            }
        });
    }

    /**
     * Replaces the rows of the table of events and shows the event that is selected after it. The row
     * of the same event stays selected. While the rows are replaced the selection is empty for a
     * moment; the details component is not told about it, so it does not clear what the user typed.
     */
    protected <S> void replaceEventRows(TableView<S> table, List<S> rows, Function<S, EventInfoDTO> eventOf) {
        replacingRows = true;
        ViewUtils.replaceItems(table, rows,
                (row, selected) -> eventOf.apply(row).id() == eventOf.apply(selected).id());
        replacingRows = false;
        showEventOf(table.getSelectionModel().getSelectedItem(), eventOf);
    }

    private <S> void showEventOf(S row, Function<S, EventInfoDTO> eventOf) {
        eventDetailComponentController.showEvent(row == null ? null : eventOf.apply(row));
    }
}