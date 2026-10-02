package gm.ui.fx.events;

import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.dto.UserDetailsDTO;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.Skin;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailScreen;
import gm.ui.fx.newevent.NewEventController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The events screen: the events of the system, filtered by type, status and commission method, and
 * the details of the selected event. The filtering itself is done by the engine - this screen only
 * sends the selected values.
 * <p>
 * It is also where a user creates an event of their own (bonus), through the form of
 * {@link NewEventController}.
 */
public class EventsController extends EventDetailScreen {

    private static final String NEW_EVENT_FXML = "/gm/ui/fx/newevent/newevent.fxml";

    @FXML private FlowPane typeFilterPane;
    @FXML private FlowPane statusFilterPane;
    @FXML private FlowPane commissionFilterPane;
    @FXML private SplitPane splitPane;
    @FXML private TableView<EventInfoDTO> eventsTable;
    @FXML private TableColumn<EventInfoDTO, String> idColumn;
    @FXML private TableColumn<EventInfoDTO, String> nameColumn;
    @FXML private TableColumn<EventInfoDTO, String> statusColumn;
    @FXML private TableColumn<EventInfoDTO, String> typeColumn;
    @FXML private TableColumn<EventInfoDTO, String> commissionColumn;
    @FXML private TableColumn<EventInfoDTO, String> marketMakerColumn;
    @FXML private TableColumn<EventInfoDTO, String> balanceColumn;
    @FXML private Label eventsCountLabel;

    private int totalEventCount;
    /** Volatile: the automatic updates read it on their own thread, to ask for the same events. */
    private volatile EventFilterDTO filter;
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

        ViewUtils.keepHeadersWhole(eventsTable);
        ViewUtils.heightFollowsItems(splitPane, 0);
        ViewUtils.bindText(idColumn, event -> String.valueOf(event.id()));
        ViewUtils.bindText(nameColumn, EventInfoDTO::name);
        ViewUtils.bindText(statusColumn, event -> event.status().getDisplayName());
        ViewUtils.bindText(typeColumn, event -> event.type().getDisplayName());
        ViewUtils.bindText(commissionColumn,
                event -> Formats.commission(event.commissionPercent(), event.commissionType()));
        ViewUtils.bindText(marketMakerColumn, EventInfoDTO::marketMakerName);
        ViewUtils.bindText(balanceColumn, event -> Formats.decimal(event.accountBalance()));

        showSelectedEvent(eventsTable, Function.identity());
        applyFilters();
    }

    /**
     * Pulls the events (and the user who acts on them) from the engine again, right now. The selected
     * event stays selected when it still exists.
     */
    public void refresh() {
        showActingUser(engine.getUserDetails(userName));
        showAllEvents(engine.getAllEvents());
        applyFilters();
        eventDetailComponentController.refresh();
    }

    /**
     * @return the filter the user chose, to ask the server for the events of the table
     */
    public EventFilterDTO filter() {
        return filter;
    }

    public void showActingUser(UserDetailsDTO user) {
        eventDetailComponentController.setActingUser(user);
    }

    /**
     * @param allEvents every event of the system, whatever the filter is; only counted here
     */
    public void showAllEvents(List<EventInfoDTO> allEvents) {
        totalEventCount = allEvents.size();
        showEventsCount();
    }

    /**
     * @param visibleEvents the events that pass the filter, which are the rows of the table
     */
    public void showEvents(List<EventInfoDTO> visibleEvents) {
        replaceEventRows(eventsTable, visibleEvents, Function.identity());
        showEventsCount();
    }

    private void showEventsCount() {
        eventsCountLabel.setText("Showing " + eventsTable.getItems().size() + " of " + totalEventCount
                + " events");
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
        form.setCreatorName(eventDetailComponentController.actingUserName());

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setDialogPane(dialogPane);
        dialog.setTitle("New event");
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
            ViewUtils.selectFirst(eventsTable, event -> event.id() == created[0].id());
            Dialogs.showInformation("Event created", "'" + created[0].name()
                    + "' was created. It is inactive until you open it, and then trading begins.");
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
            Dialogs.showError("Event not created", refused);
            return null;
        }
    }

    private void applyFilters() {
        filter = new EventFilterDTO(typeFilter.selectedValues(), statusFilter.selectedValues(),
                commissionFilter.selectedValues());
        showEvents(engine != null ? engine.getEvents(filter) : List.of());
    }
}
