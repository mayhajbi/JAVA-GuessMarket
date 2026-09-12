package gm.ui.fx.events;

import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.Skin;
import gm.ui.fx.app.AppController;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailController;
import gm.ui.fx.newevent.NewEventController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * The events screen: the events of the system, filtered by type, status and commission method, and
 * the details of the selected event. The filtering itself is done by the engine - this screen only
 * sends the selected values.
 * <p>
 * It is also where a user creates an event of their own (bonus), through the form of
 * {@link NewEventController}.
 */
public class EventsController {

    private static final String NEW_EVENT_FXML = "/gm/ui/fx/newevent/newevent.fxml";

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
    @FXML private Button newEventButton;
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
        // There is nobody to create an event on behalf of until a file was loaded.
        newEventButton.setDisable(true);
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
        newEventButton.setDisable(false);
        applyFilters();
    }

    /**
     * Opens the form of a new event and creates it. The form only closes once the engine accepted
     * the event, so a rejected event keeps whatever was already typed into it.
     */
    @FXML
    private void onNewEvent() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(NEW_EVENT_FXML));
        DialogPane dialogPane;
        try {
            dialogPane = loader.load();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Skin.dress(dialogPane);
        NewEventController form = loader.getController();
        form.setUsers(engine.getAllUsers(), eventDetailComponentController.actingUserName());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setTitle("New event");
        dialog.setHeaderText("The user who creates the event becomes its market maker, and opens it "
                + "when it is ready.");
        dialog.setResizable(true);
        dialogPane.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        EventInfoDTO[] created = new EventInfoDTO[1];
        dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, closing -> {
            created[0] = createEvent(form);
            if (created[0] == null) {
                closing.consume();
            }
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isPresent() && answer.get() == ButtonType.OK && created[0] != null) {
            refresh();
            reselect(created[0].id());
            Dialogs.showInformation("The event was created", "[" + created[0].name() + "] (id "
                    + created[0].id() + ") was created by " + created[0].marketMakerName()
                    + ", who is now its market maker. The event is inactive until "
                    + created[0].marketMakerName() + " opens it.");
        }
    }

    /**
     * @return the event the form describes, or {@code null} when the form or the engine refused it -
     *         in both cases the user was already told why
     */
    private EventInfoDTO createEvent(NewEventController form) {
        NewEventRequestDTO request = form.toRequest();
        if (request == null) {
            return null;
        }
        try {
            return engine.createEvent(request);
        } catch (RuntimeException refused) {
            Dialogs.showError("The event could not be created", refused);
            return null;
        }
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
