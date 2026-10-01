package gm.client.account;

import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailScreen;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * The account screen: the balance of the user who is logged in, the events the user is connected to (as
 * market maker or participant), and the details of one of them.
 * <p>
 * The rest of the account screen (the account lines, loading funds, the other users, the balance graph and
 * uploading a file) is added with the account screen step of exercise 3.
 */
public class AccountController extends EventDetailScreen {

    @FXML private Label userNameLabel;
    @FXML private Label blockedLabel;
    @FXML private Label balanceLabel;
    @FXML private TableView<UserEventDTO> userEventsTable;
    @FXML private TableColumn<UserEventDTO, String> userEventNameColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventStatusColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventTypeColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventRoleColumn;

    @FXML
    private void initialize() {
        ViewUtils.bindText(userEventNameColumn, row -> row.event().name());
        ViewUtils.bindText(userEventStatusColumn, row -> row.event().status().getDisplayName());
        ViewUtils.bindText(userEventTypeColumn, row -> row.event().type().getDisplayName());
        ViewUtils.bindText(userEventRoleColumn, AccountController::describeRole);

        userEventsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) ->
                        eventDetailComponentController.showEvent(selected == null ? null : selected.event()));
    }

    /**
     * Pulls the details of the user from the engine again. The selected event stays selected when the
     * user is still connected to it.
     */
    public void refresh() {
        UserDetailsDTO details = engine.getUserDetails(userName);
        userNameLabel.setText(details.name());
        balanceLabel.setText(Formats.decimal(details.balance()));
        ViewUtils.show(blockedLabel, details.blocked());
        eventDetailComponentController.setActingUser(details);
        ViewUtils.replaceItems(userEventsTable, details.events(),
                (row, selected) -> row.event().id() == selected.event().id());
    }

    private static String describeRole(UserEventDTO row) {
        if (row.marketMaker() && row.participant()) {
            return "Market maker, participant";
        }
        return row.marketMaker() ? "Market maker" : "Participant";
    }
}