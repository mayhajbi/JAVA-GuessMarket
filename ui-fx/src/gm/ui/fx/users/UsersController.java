package gm.ui.fx.users;

import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.app.AppController;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * The users screen: every user with the balance, and for the selected user the events the user is
 * connected to (as market maker or participant) and the details of one of them.
 */
public class UsersController {

    @FXML private TableView<UserInfoDTO> usersTable;
    @FXML private TableColumn<UserInfoDTO, String> userNameColumn;
    @FXML private TableColumn<UserInfoDTO, String> userBalanceColumn;
    @FXML private TableColumn<UserInfoDTO, String> userBlockedColumn;
    @FXML private Label userPlaceholderLabel;
    @FXML private VBox userDetailsBox;
    @FXML private Label userNameLabel;
    @FXML private Label blockedLabel;
    @FXML private Label balanceLabel;
    @FXML private TableView<UserEventDTO> userEventsTable;
    @FXML private TableColumn<UserEventDTO, String> userEventNameColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventStatusColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventTypeColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventRoleColumn;
    @FXML private EventDetailController eventDetailComponentController;

    private GuessMarketEngine engine;

    @FXML
    private void initialize() {
        ViewUtils.bindText(userNameColumn, UserInfoDTO::name);
        ViewUtils.bindText(userBalanceColumn, user -> Formats.decimal(user.balance()));
        ViewUtils.bindText(userBlockedColumn, user -> user.blocked() ? "Blocked" : "");

        ViewUtils.bindText(userEventNameColumn, row -> row.event().name());
        ViewUtils.bindText(userEventStatusColumn, row -> row.event().status().getDisplayName());
        ViewUtils.bindText(userEventTypeColumn, row -> row.event().type().getDisplayName());
        ViewUtils.bindText(userEventRoleColumn, UsersController::describeRole);

        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showUser(selected));
        userEventsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showUserEvent(selected));
        // On this screen every action is performed by the selected user.
        eventDetailComponentController.setFixedActingUser(null);
        showUser(null);
    }

    public void setMainController(AppController mainController) {
        eventDetailComponentController.setOnDataChanged(mainController::refreshAll);
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
        eventDetailComponentController.setEngine(engine);
    }

    /**
     * Pulls the users from the engine again. The selected user (and the selected event of that user)
     * stays selected when it still exists.
     */
    public void refresh() {
        UserInfoDTO selected = usersTable.getSelectionModel().getSelectedItem();
        usersTable.getSelectionModel().clearSelection();
        usersTable.getItems().setAll(engine.getAllUsers());
        if (selected != null) {
            for (UserInfoDTO user : usersTable.getItems()) {
                if (user.name().equals(selected.name())) {
                    usersTable.getSelectionModel().select(user);
                    return;
                }
            }
        }
    }

    private void showUser(UserInfoDTO user) {
        ViewUtils.show(userPlaceholderLabel, user == null);
        ViewUtils.show(userDetailsBox, user != null);
        UserEventDTO selectedEvent = userEventsTable.getSelectionModel().getSelectedItem();
        userEventsTable.getSelectionModel().clearSelection();
        if (user == null) {
            userEventsTable.getItems().clear();
            return;
        }

        eventDetailComponentController.setFixedActingUser(user);
        UserDetailsDTO details = engine.getUserDetails(user.name());
        userNameLabel.setText(details.name());
        balanceLabel.setText(Formats.decimal(details.balance()));
        ViewUtils.show(blockedLabel, details.blocked());
        userEventsTable.getItems().setAll(details.events());
        if (selectedEvent != null) {
            for (UserEventDTO row : userEventsTable.getItems()) {
                if (row.event().id() == selectedEvent.event().id()) {
                    userEventsTable.getSelectionModel().select(row);
                    return;
                }
            }
        }
    }

    private void showUserEvent(UserEventDTO row) {
        if (row == null) {
            eventDetailComponentController.clear();
        } else {
            eventDetailComponentController.showEvent(row.event());
        }
    }

    private static String describeRole(UserEventDTO row) {
        if (row.marketMaker() && row.participant()) {
            return "Market maker, participant";
        }
        return row.marketMaker() ? "Market maker" : "Participant";
    }
}
