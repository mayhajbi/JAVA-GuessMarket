package gm.ui.fx.eventdetail;

import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderBookTradeDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.OrderSide;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The details of a single event and the actions that can be performed on it. Used by both screens:
 * on the events screen the acting user is chosen out of all the users, and on the users screen the
 * selected user is the one who acts.
 * <p>
 * An LMSR event shows its option values and trading history; an order book event shows the order
 * book of every option, its participants and trades, and the position of the acting user. Which
 * actions are offered depends on the event and the acting user, only to guide the user - the engine
 * checks every rule again and reports a detailed failure if one is broken.
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
    @FXML private Label openHintLabel;
    @FXML private HBox buyBox;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> buyOptionComboBox;
    @FXML private Button buyButton;
    @FXML private FlowPane orderBox;
    @FXML private RadioButton orderBuyToggle;
    @FXML private TextField orderQuantityField;
    @FXML private ComboBox<String> orderOptionComboBox;
    @FXML private TextField orderPriceField;
    @FXML private Button placeOrderButton;
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

    @FXML private VBox orderBookBox;
    @FXML private Label orderBookInfoLabel;
    @FXML private VBox positionBox;
    @FXML private Label positionTitleLabel;
    @FXML private GridPane positionGrid;
    @FXML private HBox optionBooksBox;
    @FXML private TableView<OrderBookParticipantDTO> participantsTable;
    @FXML private TableView<OrderBookTradeDTO> orderBookTradesTable;
    @FXML private TableColumn<OrderBookTradeDTO, String> obBuyerColumn;
    @FXML private TableColumn<OrderBookTradeDTO, String> obFromColumn;
    @FXML private TableColumn<OrderBookTradeDTO, String> obOptionColumn;
    @FXML private TableColumn<OrderBookTradeDTO, String> obSharesColumn;
    @FXML private TableColumn<OrderBookTradeDTO, String> obPriceColumn;
    @FXML private TableColumn<OrderBookTradeDTO, String> obCommissionColumn;

    private final List<OptionBookView> optionBookViews = new ArrayList<>();
    private GuessMarketEngine engine;
    private Runnable onDataChanged = () -> { };
    private List<UserInfoDTO> users = List.of();
    private boolean isActingUserFixed;
    private UserInfoDTO fixedActingUser;
    private EventInfoDTO currentEvent;
    /** The state of the shown order book event; {@code null} while an LMSR event is shown. */
    private OrderBookStateDTO currentOrderBook;

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

        ViewUtils.bindText(obBuyerColumn, OrderBookTradeDTO::buyerName);
        ViewUtils.bindText(obFromColumn, trade -> trade.minted()
                ? "Minted with " + trade.counterpartyName()
                : trade.counterpartyName());
        ViewUtils.bindText(obOptionColumn, OrderBookTradeDTO::optionName);
        ViewUtils.bindText(obSharesColumn, trade -> String.valueOf(trade.quantity()));
        ViewUtils.bindText(obPriceColumn, trade -> Formats.decimal(trade.price()));
        ViewUtils.bindText(obCommissionColumn, trade -> Formats.decimal(trade.commissionPaid()));

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
            orderQuantityField.clear();
            orderPriceField.clear();
        }

        boolean isLmsr = event.type() == EventType.LMSR;
        if (isLmsr) {
            MarketStateDTO state = engine.getMarketState(event.id());
            currentEvent = state.eventInfo();
            currentOrderBook = null;
            showSummary(state.totalCommissionCollected(), state.winningOption().orElse(NOT_AVAILABLE));
            optionsTable.getItems().setAll(state.optionStates());
            historyTable.getItems().setAll(state.tradeHistory());
        } else {
            OrderBookStateDTO state = engine.getOrderBookState(event.id());
            currentEvent = state.eventInfo();
            currentOrderBook = state;
            showSummary(state.totalCommissionCollected(), state.winningOption().orElse(NOT_AVAILABLE));
            showOrderBook(state);
        }
        ViewUtils.show(lmsrBox, isLmsr);
        ViewUtils.show(orderBookBox, !isLmsr);
        fillOptionChoices(currentEvent.optionNames());
        updateActions();
    }

    public void clear() {
        currentEvent = null;
        currentOrderBook = null;
        ViewUtils.show(placeholderLabel, true);
        ViewUtils.show(detailsBox, false);
        optionsTable.getItems().clear();
        historyTable.getItems().clear();
        participantsTable.getItems().clear();
        orderBookTradesTable.getItems().clear();
    }

    @FXML
    private void onOpen() {
        String marketMakerName = actingUser().name();
        String eventName = currentEvent.name();
        int eventId = currentEvent.id();
        boolean isLmsr = currentEvent.type() == EventType.LMSR;
        EventInfoDTO openedEvent = engine.openEvent(eventId, marketMakerName);
        onDataChanged.run();

        String paid = Formats.decimal(openedEvent.accountBalance());
        if (isLmsr) {
            Dialogs.showInformation("The event was opened", marketMakerName + " opened [" + eventName
                    + "] and paid the initial subsidy of " + paid + ". Trading in the event is now allowed.");
        } else {
            long pairs = findParticipant(engine.getOrderBookState(eventId), marketMakerName)
                    .sharesPerOption().get(0);
            Dialogs.showInformation("The event was opened", marketMakerName + " opened [" + eventName
                    + "], paid the initial investment of " + paid + " and received " + pairs
                    + " shares of every option, which may now be offered for sale. Trading in the event "
                    + "is now allowed.");
        }
    }

    @FXML
    private void onBuy() {
        int optionIndex = buyOptionComboBox.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            Dialogs.showWarning("Buying shares", "Please choose the option you would like to buy.");
            return;
        }
        Long quantity = parseQuantity(quantityField, "Buying shares");
        if (quantity == null) {
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
        showActionResult("The purchase was completed", buyerName, details, result.buyerBlocked());
    }

    @FXML
    private void onPlaceOrder() {
        int optionIndex = orderOptionComboBox.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            Dialogs.showWarning("Placing an order",
                    "Please choose the option whose shares you would like to trade.");
            return;
        }
        Long quantity = parseQuantity(orderQuantityField, "Placing an order");
        if (quantity == null) {
            return;
        }
        String priceText = orderPriceField.getText().trim();
        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException exception) {
            Dialogs.showWarning("Placing an order", "The price [" + priceText + "] is not a number. "
                    + "Please enter the price per share, for example 0.45.");
            return;
        }

        String userName = actingUser().name();
        OrderSide side = orderBuyToggle.isSelected() ? OrderSide.BUY : OrderSide.SELL;
        String optionName = currentEvent.optionNames().get(optionIndex);
        OrderResultDTO result = engine.submitOrder(
                new OrderRequestDTO(currentEvent.id(), userName, side, optionIndex, quantity, price));
        orderQuantityField.clear();
        orderPriceField.clear();
        onDataChanged.run();

        showActionResult("The order was placed", userName,
                describeOrderResult(userName, side, optionName, quantity, price, result),
                result.userBlocked());
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

    private Long parseQuantity(TextField field, String action) {
        String text = field.getText().trim();
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException exception) {
            Dialogs.showWarning(action, "The quantity [" + text + "] is not a whole number. Please enter "
                    + "the amount of shares as a positive whole number, for example 10.");
            return null;
        }
    }

    /**
     * Reports a completed purchase or order. When it brought the balance of the user below zero, the
     * report is a warning that the user is now blocked.
     */
    private void showActionResult(String header, String userName, String details, boolean isUserBlocked) {
        if (isUserBlocked) {
            Dialogs.showWarning(header + " - " + userName + " is now blocked", details
                    + "\n\nThe balance dropped below zero, so " + userName + " is blocked from opening "
                    + "events, buying shares and placing orders from now on.");
        } else {
            Dialogs.showInformation(header, details);
        }
    }

    /**
     * Describes the trades of an order from the side of the user who placed it.
     */
    private String describeOrderResult(String userName, OrderSide side, String optionName, long quantity,
                                       double price, OrderResultDTO result) {
        StringBuilder text = new StringBuilder(userName + " placed an order to "
                + side.getDisplayName().toLowerCase(Locale.ROOT) + " " + quantity + " shares of ["
                + optionName + "] at " + Formats.decimal(price) + ".\n");

        List<String> tradeLines = new ArrayList<>();
        for (OrderBookTradeDTO trade : result.trades()) {
            String commission = trade.commissionPaid() > 0
                    ? " (commission " + Formats.decimal(trade.commissionPaid()) + ")"
                    : "";
            if (side == OrderSide.BUY && trade.buyerName().equals(userName)) {
                tradeLines.add(trade.minted()
                        ? "  " + trade.quantity() + " new shares minted at " + Formats.decimal(trade.price())
                                + ", paired with the order of " + trade.counterpartyName() + commission
                        : "  " + trade.quantity() + " shares bought from " + trade.counterpartyName() + " at "
                                + Formats.decimal(trade.price()) + commission);
            } else if (side == OrderSide.SELL && trade.counterpartyName().equals(userName) && !trade.minted()) {
                tradeLines.add("  " + trade.quantity() + " shares sold to " + trade.buyerName() + " at "
                        + Formats.decimal(trade.price()));
            }
        }
        if (tradeLines.isEmpty()) {
            text.append("No matching order was found yet.\n");
        } else {
            text.append("Trades:\n").append(String.join("\n", tradeLines)).append("\n");
        }
        text.append("Matched ").append(result.filledQuantity()).append(" of ").append(quantity)
                .append(" shares; ").append(result.restingQuantity()).append(" wait in the order book.\n")
                .append("Balance of ").append(userName).append(": ")
                .append(Formats.decimal(result.userBalance()));
        return text.toString();
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
        ViewUtils.show(openBox, status == EventStatus.INACTIVE);
        openHintLabel.setText(isLmsr
                ? "The market maker pays the initial subsidy and trading starts."
                : "The market maker pays the initial investment, receives the initial shares and trading "
                        + "starts.");
        ViewUtils.show(buyBox, isLmsr && status == EventStatus.ACTIVE);
        ViewUtils.show(orderBox, !isLmsr && status == EventStatus.ACTIVE);
        ViewUtils.show(closeBox, status == EventStatus.ACTIVE && isMarketMaker);
        openButton.setDisable(!isMarketMaker || isBlocked);
        buyButton.setDisable(user == null || isBlocked);
        placeOrderButton.setDisable(user == null || isBlocked);

        String hint = describeAvailableActions(user, status, isMarketMaker, isBlocked);
        actionsHintLabel.setText(hint);
        ViewUtils.show(actionsHintLabel, !hint.isEmpty());
        showPosition(user);
    }

    private String describeAvailableActions(UserInfoDTO user, EventStatus status, boolean isMarketMaker,
                                            boolean isBlocked) {
        if (status == EventStatus.CLOSED) {
            return "This event is closed - no further actions are possible.";
        }
        if (user == null) {
            return "Choose the user who performs the action.";
        }
        if (isBlocked) {
            return user.name() + " is blocked (the balance dropped below zero) and cannot open events, "
                    + "buy shares or place orders." + (isMarketMaker && status == EventStatus.ACTIVE
                    ? " As the market maker, " + user.name() + " may still close this event."
                    : "");
        }
        if (status == EventStatus.INACTIVE && !isMarketMaker) {
            return "This event is not open yet. Only its market maker ["
                    + currentEvent.marketMakerName() + "] can open it.";
        }
        return "";
    }

    private void showOrderBook(OrderBookStateDTO state) {
        orderBookInfoLabel.setText("Base value (d): " + state.baseValue() + " - every winning share pays "
                + state.baseValue() + ". Order prices from 0.01 to " + Formats.decimal(state.baseValue() - 0.01)
                + ". Minting: " + (state.mintAllowed() ? "allowed" : "not allowed")
                + ". Initial investment of the market maker: " + state.initialInvestment() + ".");

        while (optionBookViews.size() < state.options().size()) {
            OptionBookView view = new OptionBookView();
            optionBookViews.add(view);
            optionBooksBox.getChildren().add(view);
        }
        for (int index = 0; index < state.options().size(); index++) {
            optionBookViews.get(index).show(index + 1, state.options().get(index));
        }

        rebuildParticipantColumns(state.eventInfo().optionNames());
        participantsTable.getItems().setAll(state.participants());
        orderBookTradesTable.getItems().setAll(state.tradeHistory());
    }

    /**
     * The participants table has two columns (shares and value) for every option, named after the
     * options of the shown event.
     */
    private void rebuildParticipantColumns(List<String> optionNames) {
        List<String> headers = new ArrayList<>();
        headers.add("User");
        for (String optionName : optionNames) {
            headers.add(optionName + " shares");
            headers.add(optionName + " value");
        }
        List<String> currentHeaders = new ArrayList<>();
        for (TableColumn<OrderBookParticipantDTO, ?> column : participantsTable.getColumns()) {
            currentHeaders.add(column.getText());
        }
        if (currentHeaders.equals(headers)) {
            return;
        }

        List<TableColumn<OrderBookParticipantDTO, String>> columns = new ArrayList<>();
        TableColumn<OrderBookParticipantDTO, String> userColumn = new TableColumn<>("User");
        ViewUtils.bindText(userColumn, OrderBookParticipantDTO::userName);
        columns.add(userColumn);
        for (int index = 0; index < optionNames.size(); index++) {
            int optionIndex = index;
            TableColumn<OrderBookParticipantDTO, String> sharesColumn =
                    new TableColumn<>(optionNames.get(index) + " shares");
            ViewUtils.bindText(sharesColumn,
                    participant -> String.valueOf(participant.sharesPerOption().get(optionIndex)));
            TableColumn<OrderBookParticipantDTO, String> valueColumn =
                    new TableColumn<>(optionNames.get(index) + " value");
            ViewUtils.bindText(valueColumn,
                    participant -> Formats.optionalDecimal(participant.holdingValuePerOption().get(optionIndex)));
            sharesColumn.getStyleClass().add("numeric");
            valueColumn.getStyleClass().add("numeric");
            columns.add(sharesColumn);
            columns.add(valueColumn);
        }
        participantsTable.getColumns().setAll(columns);
    }

    /**
     * Shows what the acting user holds, paid and received in the shown order book event, when the
     * acting user takes part in it.
     */
    private void showPosition(UserInfoDTO user) {
        OrderBookParticipantDTO position = currentOrderBook == null || user == null
                ? null
                : findParticipant(currentOrderBook, user.name());
        ViewUtils.show(positionBox, position != null);
        if (position == null) {
            return;
        }

        positionTitleLabel.setText("Position of " + user.name());
        positionGrid.getChildren().clear();
        List<String> optionNames = currentEvent.optionNames();
        int row = 0;
        for (int index = 0; index < optionNames.size(); index++) {
            addPositionRow(row++, (index + 1) + ". " + optionNames.get(index),
                    position.sharesPerOption().get(index) + " shares, value "
                            + Formats.optionalDecimal(position.holdingValuePerOption().get(index))
                            + ", paid " + Formats.decimal(position.paidPerOption().get(index)));
        }
        if (position.initialInvestmentPaid() > 0) {
            addPositionRow(row++, "Initial investment paid", Formats.decimal(position.initialInvestmentPaid()));
        }
        addPositionRow(row++, "Commission paid", Formats.decimal(position.commissionPaid()));
        addPositionRow(row++, "Received (sales and payout)", Formats.decimal(position.received()));
        addPositionRow(row, "Profit / loss", currentEvent.status() == EventStatus.CLOSED
                ? Formats.decimal(position.profitOrLoss())
                : "known when the event is closed");
    }

    private void addPositionRow(int row, String name, String value) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("field-name");
        positionGrid.add(nameLabel, 0, row);
        positionGrid.add(new Label(value), 1, row);
    }

    private static OrderBookParticipantDTO findParticipant(OrderBookStateDTO state, String userName) {
        for (OrderBookParticipantDTO participant : state.participants()) {
            if (participant.userName().equals(userName)) {
                return participant;
            }
        }
        return null;
    }

    private void fillOptionChoices(List<String> optionNames) {
        List<String> numberedNames = new ArrayList<>();
        for (int index = 0; index < optionNames.size(); index++) {
            numberedNames.add((index + 1) + ". " + optionNames.get(index));
        }
        // Refilling a choice box clears its selection, so it is refilled only for a different event.
        if (!buyOptionComboBox.getItems().equals(numberedNames)) {
            buyOptionComboBox.getItems().setAll(numberedNames);
            orderOptionComboBox.getItems().setAll(numberedNames);
            winnerComboBox.getItems().setAll(numberedNames);
        }
    }

    private void showSummary(double commissionCollected, String winner) {
        eventNameLabel.setText(currentEvent.name() + " (id " + currentEvent.id() + ")");
        descriptionLabel.setText(currentEvent.description());
        statusValue.setText(currentEvent.status().getDisplayName());
        typeValue.setText(currentEvent.type().getDisplayName());
        commissionValue.setText(
                Formats.commission(currentEvent.commissionPercent(), currentEvent.commissionType()));
        marketMakerValue.setText(currentEvent.marketMakerName());
        balanceValue.setText(Formats.decimal(currentEvent.accountBalance()));
        commissionCollectedValue.setText(Formats.decimal(commissionCollected));
        winnerValue.setText(winner);
    }
}
