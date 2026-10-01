package gm.client.account;

import gm.client.task.LoadEventsTask;
import gm.dto.AccountEntryDTO;
import gm.dto.UploadResultDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.HistoryChart;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailScreen;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Map;

/**
 * The account screen: the balance of the user who is logged in and how it changed, the other users, the
 * events the user is connected to (as market maker or participant) and the details of one of them. The
 * user can also load funds and upload a file of events.
 */
public class AccountController extends EventDetailScreen {

    private static final String BALANCE_SERIES = "Balance";

    @FXML private Button loadFileButton;
    @FXML private TextField filePathField;
    @FXML private ProgressBar uploadProgress;
    @FXML private Label uploadMessageLabel;

    @FXML private TableView<UserInfoDTO> usersTable;
    @FXML private TableColumn<UserInfoDTO, String> userNameColumn;
    @FXML private TableColumn<UserInfoDTO, String> userBalanceColumn;
    @FXML private TableColumn<UserInfoDTO, String> userMarketMakerColumn;

    @FXML private TableView<AccountEntryDTO> accountEntriesTable;
    @FXML private TableColumn<AccountEntryDTO, String> entryTypeColumn;
    @FXML private TableColumn<AccountEntryDTO, String> entryAmountColumn;
    @FXML private TableColumn<AccountEntryDTO, String> entryBalanceColumn;

    @FXML private Label userNameLabel;
    @FXML private Label blockedLabel;
    @FXML private Label balanceLabel;
    @FXML private LineChart<Number, Number> balanceChart;

    @FXML private TableView<UserEventDTO> userEventsTable;
    @FXML private TableColumn<UserEventDTO, String> userEventNameColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventStatusColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventTypeColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventRoleColumn;

    private Runnable onDataChanged = () -> { };

    @FXML
    private void initialize() {
        ViewUtils.show(uploadProgress, false);

        ViewUtils.bindText(userNameColumn, UserInfoDTO::name);
        ViewUtils.bindText(userBalanceColumn, user -> Formats.decimal(user.balance()));
        ViewUtils.bindText(userMarketMakerColumn, user -> user.marketMaker() ? "Yes" : "No");

        ViewUtils.bindText(entryTypeColumn, entry -> entry.type().getDisplayName());
        ViewUtils.bindText(entryAmountColumn, entry -> Formats.decimal(entry.amount()));
        ViewUtils.bindText(entryBalanceColumn, entry -> Formats.decimal(entry.balanceAfter()));

        ViewUtils.bindText(userEventNameColumn, row -> row.event().name());
        ViewUtils.bindText(userEventStatusColumn, row -> row.event().status().getDisplayName());
        ViewUtils.bindText(userEventTypeColumn, row -> row.event().type().getDisplayName());
        ViewUtils.bindText(userEventRoleColumn, AccountController::describeRole);

        userEventsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) ->
                        eventDetailComponentController.showEvent(selected == null ? null : selected.event()));
    }

    @Override
    public void setOnDataChanged(Runnable onDataChanged) {
        super.setOnDataChanged(onDataChanged);
        this.onDataChanged = onDataChanged;
    }

    /**
     * Pulls the data of the screen from the engine again. The selected event stays selected when the
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

        // The engine of the client lists the user who is logged in first, then the others.
        usersTable.getItems().setAll(engine.getAllUsers().stream().skip(1).toList());
        accountEntriesTable.getItems().setAll(engine.getAccountEntries(userName));
        HistoryChart.fill(balanceChart, Map.of(BALANCE_SERIES, engine.getUserBalanceHistory(userName)));
    }

    /**
     * Uploads a file the user chooses. The upload runs in the background, so the rest of the application
     * stays usable; only another upload waits until this one is finished.
     */
    @FXML
    private void loadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a file of events");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(loadFileButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        filePathField.setText(file.getAbsolutePath());

        LoadEventsTask task = new LoadEventsTask(engine, userName, file.toPath());
        loadFileButton.disableProperty().bind(task.runningProperty());
        uploadProgress.visibleProperty().bind(task.runningProperty());
        uploadProgress.managedProperty().bind(task.runningProperty());
        uploadMessageLabel.textProperty().bind(
                Bindings.when(task.runningProperty()).then(task.messageProperty()).otherwise(""));
        task.setOnSucceeded(done -> uploadSucceeded(task.getValue()));
        task.setOnFailed(failed -> Dialogs.showError("The upload failed", task.getException()));

        Thread uploader = new Thread(task, "upload-events");
        uploader.setDaemon(true);
        uploader.start();
    }

    private void uploadSucceeded(UploadResultDTO result) {
        onDataChanged.run();
        Dialogs.showInformation("The file was uploaded",
                result.eventNames().size() + " events added from " + result.fileName() + ".");
    }

    /**
     * Asks for an amount and adds it to the balance of the user.
     */
    @FXML
    private void loadFunds() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Load funds");
        dialog.setHeaderText("Load funds");
        dialog.setContentText("Amount:");
        dialog.showAndWait().ifPresent(text -> {
            Double amount = ViewUtils.readNumber(dialog.getEditor(), Double::parseDouble, "Load funds",
                    invalid -> "'" + invalid + "' is not a number.");
            if (amount == null) {
                return;
            }
            try {
                engine.deposit(userName, amount);
                onDataChanged.run();
            } catch (RuntimeException refused) {
                Dialogs.showError("Load funds", refused);
            }
        });
    }

    private static String describeRole(UserEventDTO row) {
        if (row.marketMaker() && row.participant()) {
            return "Market maker, participant";
        }
        return row.marketMaker() ? "Market maker" : "Participant";
    }
}
