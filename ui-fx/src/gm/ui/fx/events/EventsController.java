package gm.ui.fx.events;

import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.app.AppController;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;

import java.util.List;

/**
 * The events screen: the events of the system, filtered by type, status and commission method, and
 * the details of the selected event. The filtering itself is done by the engine - this screen only
 * sends the selected values.
 */
public class EventsController {

    @FXML private FlowPane typeFilterPane;
    @FXML private FlowPane statusFilterPane;
    @FXML private FlowPane commissionFilterPane;
    @FXML private TableView<EventInfoDTO> eventsTable;
    @FXML private TableColumn<EventInfoDTO, String> idColumn;
    @FXML private TableColumn<EventInfoDTO, String> nameColumn;
    @FXML private TableColumn<EventInfoDTO, String> statusColumn;
    @FXML private TableColumn<EventInfoDTO, String> typeColumn;
    @FXML private TableColumn<EventInfoDTO, String> commissionColumn;
    @FXML private TableColumn<EventInfoDTO, String> marketMakerColumn;
    @FXML private TableColumn<EventInfoDTO, String> balanceColumn;
    @FXML private Label eventsCountLabel;
    @FXML private EventDetailController eventDetailComponentController;

    private GuessMarketEngine engine;
    /** Whether a file was loaded - before that the engine has no events to ask for. */
    private boolean isSystemLoaded;
    private int totalEventCount;
    private FilterGroup<EventType> typeFilter;
    private FilterGroup<EventStatus> statusFilter;
    private FilterGroup<CommissionType> commissionFilter;

    @FXML
    private void initialize() {
        typeFilter = new FilterGroup<>(typeFilterPane, EventType.values(), EventType::getDisplayName,
                this::applyFilters);
        statusFilter = new FilterGroup<>(statusFilterPane, EventStatus.values(),
                EventStatus::getDisplayName, this::applyFilters);
        commissionFilter = new FilterGroup<>(commissionFilterPane, CommissionType.values(),
                CommissionType::getDisplayName, this::applyFilters);

        ViewUtils.bindText(idColumn, event -> String.valueOf(event.id()));
        ViewUtils.bindText(nameColumn, EventInfoDTO::name);
        ViewUtils.bindText(statusColumn, event -> event.status().getDisplayName());
        ViewUtils.bindText(typeColumn, event -> event.type().getDisplayName());
        ViewUtils.bindText(commissionColumn,
                event -> Formats.commission(event.commissionPercent(), event.commissionType()));
        ViewUtils.bindText(marketMakerColumn, EventInfoDTO::marketMakerName);
        ViewUtils.bindText(balanceColumn, event -> Formats.decimal(event.accountBalance()));

        eventsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showDetails(selected));
        applyFilters();
    }

    public void setMainController(AppController mainController) {
        eventDetailComponentController.setOnDataChanged(mainController::refreshAll);
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventDetailComponentController.setEngine(engine);
    }

    /**
     * Pulls the events (and the users who may act on them) from the engine again. The selected event
     * stays selected when it still exists.
     */
    public void refresh() {
        eventDetailComponentController.setUsers(engine.getAllUsers());
        totalEventCount = engine.getAllEvents().size();
        isSystemLoaded = true;
        applyFilters();
    }

    private void applyFilters() {
        EventInfoDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        List<EventInfoDTO> visibleEvents = isSystemLoaded
                ? engine.getEvents(new EventFilterDTO(typeFilter.selectedValues(),
                        statusFilter.selectedValues(), commissionFilter.selectedValues()))
                : List.of();
        eventsTable.getSelectionModel().clearSelection();
        eventsTable.getItems().setAll(visibleEvents);
        eventsCountLabel.setText("Showing " + visibleEvents.size() + " of " + totalEventCount
                + " events");
        if (selected != null) {
            reselect(selected.id());
        }
    }

    private void reselect(int eventId) {
        for (EventInfoDTO event : eventsTable.getItems()) {
            if (event.id() == eventId) {
                eventsTable.getSelectionModel().select(event);
                return;
            }
        }
    }

    private void showDetails(EventInfoDTO event) {
        if (event == null) {
            eventDetailComponentController.clear();
        } else {
            eventDetailComponentController.showEvent(event);
        }
    }
}
