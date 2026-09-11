package gm.ui.fx.eventdetail;

import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.TradeRecordDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * The details of a single event and the actions that can be performed on it. Used by both screens:
 * on the events screen the acting user is chosen out of all the users, and on the users screen the
 * selected user is the one who acts.
 * <p>
 * Which actions are offered depends on the event and the acting user, only to guide the user - the
 * engine checks every rule again and reports a detailed failure if one is broken.
 */
public class EventDetailController {

    private static final String NOT_AVAILABLE = "-";

    @FXML private Label placeholderLabel;
    @FXML private VBox detailsBox;
    @FXML private Label eventNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusValue;
    @FXML private Label typeValue;
    @FXML private Label commissionValue;
    @FXML private Label marketMakerValue;
    @FXML private Label balanceValue;
    @FXML private Label commissionCollectedValue;
    @FXML private Label winnerValue;
    @FXML private ComboBox<String> actingUserComboBox;
    @FXML private Label actingUserLabel;
    @FXML private Label actingUserBlockedLabel;
    @FXML private HBox openBox;
    @FXML private Button openButton;
    @FXML private HBox buyBox;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> buyOptionComboBox;
    @FXML private Button buyButton;
    @FXML private HBox closeBox;
    @FXML private ComboBox<String> winnerComboBox;
    @FXML private Label actionsHintLabel;
    @FXML private VBox lmsrBox;
    @FXML private TableView<OptionStateDTO> optionsTable;
    @FXML private TableColumn<OptionStateDTO, String> optionNumberColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionNameColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionValueColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionSharesColumn;
    @FXML private TableView<TradeRecordDTO> historyTable;
    @FXML private TableColumn<TradeRecordDTO, String> buyerColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeOptionColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeSharesColumn;
    @FXML private TableColumn<TradeRecordDTO, String> sharesCostColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeCommissionColumn;
    @FXML private TableColumn<TradeRecordDTO, String> totalPaidColumn;
    @FXML private Label orderBookNoteLabel;

    private GuessMarketEngine engine;
    private Runnable onDataChanged = () -> { };
    private List<UserInfoDTO> users = List.of();
    private boolean isActingUserFixed;
    private UserInfoDTO fixedActingUser;
    private EventInfoDTO currentEvent;

    @FXML
    private void initialize() {
        // Options are presented to the user starting from 1.
        ViewUtils.bindText(optionNumberColumn, option -> "");
        optionNumberColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        ViewUtils.bindText(optionNameColumn, OptionStateDTO::name);
        ViewUtils.bindText(optionValueColumn, option -> Formats.decimal(option.value()));
        ViewUtils.bindText(optionSharesColumn, option -> String.valueOf(option.shares()));

        ViewUtils.bindText(buyerColumn, TradeRecordDTO::buyerName);
        ViewUtils.bindText(tradeOptionColumn, TradeRecordDTO::optionName);
        ViewUtils.bindText(tradeSharesColumn, trade -> String.valueOf(trade.shares()));
        ViewUtils.bindText(sharesCostColumn, trade -> Formats.decimal(trade.sharesCost()));
        ViewUtils.bindText(tradeCommissionColumn, trade -> Formats.decimal(trade.commissionPaid()));
        ViewUtils.bindText(totalPaidColumn, trade -> Formats.decimal(trade.totalPaid()));

        actingUserComboBox.valueProperty().addListener((observable, previous, chosen) -> updateActions());
        ViewUtils.show(actingUserLabel, false);
        clear();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /**
     * @param onDataChanged called after an action changed the data in the engine
     */
    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    /**
     * Lets the user choose who performs the actions out of the given users. The chosen user stays
     * chosen when it still exists.
     */
    public void setUsers(List<UserInfoDTO> users) {
        this.users = users;
        String chosenName = actingUserComboBox.getValue();
        List<String> names = new ArrayList<>();
        for (UserInfoDTO user : users) {
            names.add(user.name());
        }
        actingUserComboBox.getItems().setAll(names);
        actingUserComboBox.setValue(names.contains(chosenName) ? chosenName : null);
        updateActions();
    }

    /**
     * Makes the given user the one who performs every action, without offering a choice.
     */
    public void setFixedActingUser(UserInfoDTO user) {
        isActingUserFixed = true;
        fixedActingUser = user;
        ViewUtils.show(actingUserComboBox, false);
        ViewUtils.show(actingUserLabel, true);
        actingUserLabel.setText(user == null ? "" : user.name());
        updateActions();
    }

    /**
     * Shows the current state of an event, pulled from the engine.
     */
    public void showEvent(EventInfoDTO event) {
        ViewUtils.show(placeholderLabel, false);
        ViewUtils.show(detailsBox, true);
        if (currentEvent == null || currentEvent.id() != event.id()) {
            quantityField.clear();
        }

        boolean isLmsr = event.type() == EventType.LMSR;
        if (isLmsr) {
            MarketStateDTO state = engine.getMarketState(event.id());
            currentEvent = state.eventInfo();
            showSummary(Formats.decimal(state.totalCommissionCollected()),
                    state.winningOption().orElse(NOT_AVAILABLE));
            optionsTable.getItems().setAll(state.optionStates());
            historyTable.getItems().setAll(state.tradeHistory());
        } else {
            currentEvent = event;
            showSummary(NOT_AVAILABLE, NOT_AVAILABLE);
        }
        ViewUtils.show(lmsrBox, isLmsr);
        ViewUtils.show(orderBookNoteLabel, !isLmsr);
        fillOptionChoices(currentEvent.optionNames());
        updateActions();
    }

    public void clear() {
        currentEvent = null;
        ViewUtils.show(placeholderLabel, true);
        ViewUtils.show(detailsBox, false);
        optionsTable.getItems().clear();
        historyTable.getItems().clear();
    }

    @FXML
    private void onOpen() {
        String marketMakerName = actingUser().name();
        String eventName = currentEvent.name();
        MarketStateDTO state = engine.openEvent(currentEvent.id(), marketMakerName);
        onDataChanged.run();
        Dialogs.showInformation("The event was opened", marketMakerName + " opened [" + eventName
                + "] and paid the initial subsidy of " + Formats.decimal(state.accountBalance())
                + ". Trading in the event is now allowed.");
    }

    @FXML
    private void onBuy() {
        int optionIndex = buyOptionComboBox.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            Dialogs.showWarning("Buying shares", "Please choose the option you would like to buy.");
            return;
        }
        String quantityText = quantityField.getText().trim();
        long quantity;
        try {
            quantity = Long.parseLong(quantityText);
        } catch (NumberFormatException exception) {
            Dialogs.showWarning("Buying shares", "The quantity [" + quantityText + "] is not a whole "
                    + "number. Please enter the amount of shares to buy as a positive whole number, "
                    + "for example 10.");
            return;
        }

        String buyerName = actingUser().name();
        PurchaseResultDTO result = engine.buyShares(currentEvent.id(), buyerName, optionIndex, quantity);
        quantityField.clear();
        onDataChanged.run();

        String details = buyerName + " bought " + result.shares() + " shares of ["
                + result.optionName() + "].\n"
                + "Shares cost: " + Formats.decimal(result.sharesCost()) + "\n"
                + "Commission: " + Formats.decimal(result.commissionPaid()) + "\n"
                + "Total paid: " + Formats.decimal(result.totalPaid()) + "\n"
                + "Balance of " + buyerName + ": " + Formats.decimal(result.buyerBalance());
        if (result.buyerBlocked()) {
            Dialogs.showWarning("The purchase was completed - " + buyerName + " is now blocked",
                    details + "\n\nThe balance dropped below zero, so " + buyerName
                            + " is blocked from opening events and buying shares from now on.");
        } else {
            Dialogs.showInformation("The purchase was completed", details);
        }
    }

    @FXML
    private void onClose() {
        int winnerIndex = winnerComboBox.getSelectionModel().getSelectedIndex();
        if (winnerIndex < 0) {
            Dialogs.showWarning("Closing the event",
                    "Please choose the winning option before closing the event.");
            return;
        }
        String eventName = currentEvent.name();
        String winnerName = currentEvent.optionNames().get(winnerIndex);
        if (!Dialogs.confirm("Closing the event [" + eventName + "]", "Close the event with ["
                + winnerName + "] as the winning option? The winners will be paid, and the event "
                + "cannot be traded or opened again.")) {
            return;
        }

        String marketMakerName = actingUser().name();
        engine.closeEvent(currentEvent.id(), marketMakerName, winnerIndex);
        onDataChanged.run();
        Dialogs.showInformation("The event was closed", "[" + eventName + "] was closed by "
                + marketMakerName + " with [" + winnerName + "] as the winning option. The winners "
                + "were paid, and the commission and what was left in the event account went to "
                + "the market maker.");
    }

    private UserInfoDTO actingUser() {
        if (isActingUserFixed) {
            return fixedActingUser;
        }
        for (UserInfoDTO user : users) {
            if (user.name().equals(actingUserComboBox.getValue())) {
                return user;
            }
        }
        return null;
    }

    private void updateActions() {
        if (currentEvent == null) {
            return;
        }
        UserInfoDTO user = actingUser();
        EventStatus status = currentEvent.status();
        boolean isLmsr = currentEvent.type() == EventType.LMSR;
        boolean isMarketMaker = user != null && user.name().equals(currentEvent.marketMakerName());
        boolean isBlocked = user != null && user.blocked();

        ViewUtils.show(actingUserBlockedLabel, isBlocked);
        ViewUtils.show(openBox, isLmsr && status == EventStatus.INACTIVE);
        ViewUtils.show(buyBox, isLmsr && status == EventStatus.ACTIVE);
        ViewUtils.show(closeBox, isLmsr && status == EventStatus.ACTIVE && isMarketMaker);
        openButton.setDisable(!isMarketMaker || isBlocked);
        buyButton.setDisable(user == null || isBlocked);

        String hint = describeAvailableActions(user, status, isLmsr, isMarketMaker, isBlocked);
        actionsHintLabel.setText(hint);
        ViewUtils.show(actionsHintLabel, !hint.isEmpty());
    }

    private String describeAvailableActions(UserInfoDTO user, EventStatus status, boolean isLmsr,
                                            boolean isMarketMaker, boolean isBlocked) {
        if (!isLmsr) {
            return "Trading in order book events is not available yet.";
        }
        if (status == EventStatus.CLOSED) {
            return "This event is closed - no further actions are possible.";
        }
        if (user == null) {
            return "Choose the user who performs the action.";
        }
        if (isBlocked) {
            return user.name() + " is blocked (the balance dropped below zero) and cannot open events "
                    + "or buy shares." + (isMarketMaker && status == EventStatus.ACTIVE
                    ? " As the market maker, " + user.name() + " may still close this event."
                    : "");
        }
        if (status == EventStatus.INACTIVE && !isMarketMaker) {
            return "This event is not open yet. Only its market maker ["
                    + currentEvent.marketMakerName() + "] can open it.";
        }
        return "";
    }

    private void fillOptionChoices(List<String> optionNames) {
        List<String> numberedNames = new ArrayList<>();
        for (int index = 0; index < optionNames.size(); index++) {
            numberedNames.add((index + 1) + ". " + optionNames.get(index));
        }
        // Refilling a choice box clears its selection, so it is refilled only for a different event.
        if (!buyOptionComboBox.getItems().equals(numberedNames)) {
            buyOptionComboBox.getItems().setAll(numberedNames);
            winnerComboBox.getItems().setAll(numberedNames);
        }
    }

    private void showSummary(String commissionCollected, String winner) {
        eventNameLabel.setText(currentEvent.name() + " (id " + currentEvent.id() + ")");
        descriptionLabel.setText(currentEvent.description());
        statusValue.setText(currentEvent.status().getDisplayName());
        typeValue.setText(currentEvent.type().getDisplayName());
        commissionValue.setText(
                Formats.commission(currentEvent.commissionPercent(), currentEvent.commissionType()));
        marketMakerValue.setText(currentEvent.marketMakerName());
        balanceValue.setText(Formats.decimal(currentEvent.accountBalance()));
        commissionCollectedValue.setText(commissionCollected);
        winnerValue.setText(winner);
    }
}
