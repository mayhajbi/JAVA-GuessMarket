package gm.ui.fx.eventdetail;

import gm.dto.EventInfoDTO;
import gm.dto.EventStateDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderBookTradeDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.OrderSide;
import gm.dto.PriceHistoryDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.TradeRecordDTO;
import gm.dto.UserDetailsDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Animations;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.HistoryChart;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The details of a single event and the actions that can be performed on it. The acting user is always
 * the user who is logged in.
 * <p>
 * An LMSR event shows its option values and trading history; an order book event shows the order
 * book of every option, its participants and trades, and the position of the acting user. Which
 * actions are offered depends on the event and the acting user, only to guide the user - the engine
 * checks every rule again and reports a detailed failure if one is broken.
 */
public class EventDetailController {

    private static final String CHOOSE_USER = "No user is logged in.";
    private static final String BUYING_HEADER = "Buying shares";
    private static final String ORDER_HEADER = "Placing an order";

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
    @FXML private VBox lmsrHistoryBox;
    @FXML private TableView<OptionStateDTO> optionsTable;
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
    @FXML private VBox orderBookHistoryBox;
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

    @FXML private Label priceChartPlaceholder;
    @FXML private LineChart<Number, Number> priceChart;

    private final List<OptionBookView> optionBookViews = new ArrayList<>();
    private GuessMarketEngine engine;
    private Runnable onDataChanged = () -> { };
    /** The user who is logged in and performs every action; {@code null} until the first refresh. */
    private UserDetailsDTO actingUser;
    /** Volatile: the automatic updates read it on their own thread, to know which event to ask about. */
    private volatile EventInfoDTO currentEvent;
    /** The state that is shown now; {@code null} until the first state of the shown event arrives. */
    private EventStateDTO shownState;
    /** The state of the shown order book event; {@code null} while an LMSR event is shown. */
    private OrderBookStateDTO currentOrderBook;
    /**
     * The event whose details slid in last. Kept apart from {@link #currentEvent}, which is cleared
     * whenever no event is selected - so only a genuinely different event slides in.
     */
    private Integer lastSlidEventId;

    @FXML
    private void initialize() {
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
     * Sets the user who performs every action here: the user who is logged in. Called on every refresh,
     * so that the balance and the blocked state are always the current ones.
     */
    public void setActingUser(UserDetailsDTO user) {
        if (user.equals(actingUser)) {
            return;
        }
        actingUser = user;
        actingUserLabel.setText(user.name());
        updateActions();
    }

    /**
     * Chooses the event whose details are shown, and pulls them from the engine. An event that is
     * already shown stays as it is: it is kept current by {@link #refresh()} and by the automatic
     * updates.
     *
     * @param event the event to show, or {@code null} to clear the details when no event is selected
     */
    public void showEvent(EventInfoDTO event) {
        if (event == null) {
            clear();
            return;
        }
        if (currentEvent != null && currentEvent.id() == event.id()) {
            return;
        }
        quantityField.clear();
        orderQuantityField.clear();
        orderPriceField.clear();
        currentEvent = event;
        shownState = null;
        refresh();
    }

    /**
     * @return the event whose details are shown, or {@code null} when none is
     */
    public EventInfoDTO shownEvent() {
        return currentEvent;
    }

    /**
     * Pulls the details of the shown event from the engine again, right now.
     */
    public void refresh() {
        EventInfoDTO event = currentEvent;
        if (event == null) {
            return;
        }
        showState(event.type() == EventType.LMSR
                ? engine.getMarketState(event.id())
                : engine.getOrderBookState(event.id()));
        showPrices(engine.getEventPriceHistory(event.id()));
    }

    /**
     * Shows the state of the shown event. A state that is exactly the one already shown changes
     * nothing on the screen.
     */
    public void showState(EventStateDTO state) {
        if (currentEvent == null || state.equals(shownState)) {
            return;
        }
        shownState = state;
        ViewUtils.show(placeholderLabel, false);
        ViewUtils.show(detailsBox, true);

        boolean isLmsr = state instanceof MarketStateDTO;
        if (state instanceof MarketStateDTO marketState) {
            ViewUtils.replaceItems(optionsTable, marketState.optionStates());
            ViewUtils.replaceItems(historyTable, marketState.tradeHistory());
            currentOrderBook = null;
        } else {
            OrderBookStateDTO orderBookState = (OrderBookStateDTO) state;
            showOrderBook(orderBookState);
            currentOrderBook = orderBookState;
        }
        currentEvent = state.eventInfo();
        showSummary(state);
        ViewUtils.show(lmsrBox, isLmsr);
        ViewUtils.show(lmsrHistoryBox, isLmsr);
        ViewUtils.show(orderBookBox, !isLmsr);
        ViewUtils.show(orderBookHistoryBox, !isLmsr);
        fillOptionChoices(currentEvent.optionNames());
        updateActions();
        if (lastSlidEventId == null || lastSlidEventId != currentEvent.id()) {
            lastSlidEventId = currentEvent.id();
            Animations.slideIn(detailsBox);
        }
    }

    /**
     * Draws the value of every option of the shown event over time, one line per option. An event
     * that was never opened, and an order book option that was never quoted, have nothing to draw.
     */
    public void showPrices(List<PriceHistoryDTO> priceHistories) {
        if (currentEvent == null) {
            return;
        }
        Map<String, List<HistoryPointDTO>> pointsByOption = new LinkedHashMap<>();
        for (PriceHistoryDTO series : priceHistories) {
            pointsByOption.put(series.optionName(), series.points());
        }
        boolean hasPrices = HistoryChart.fill(priceChart, pointsByOption);
        ViewUtils.show(priceChart, hasPrices);
        ViewUtils.show(priceChartPlaceholder, !hasPrices);
    }

    public void clear() {
        currentEvent = null;
        currentOrderBook = null;
        shownState = null;
        ViewUtils.show(placeholderLabel, true);
        ViewUtils.show(detailsBox, false);
        optionsTable.getItems().clear();
        historyTable.getItems().clear();
        participantsTable.getItems().clear();
        orderBookTradesTable.getItems().clear();
        HistoryChart.fill(priceChart, Map.of());
    }

    @FXML
    private void onOpen() {
        String marketMakerName = actingUser.name();
        String eventName = currentEvent.name();
        int eventId = currentEvent.id();
        boolean isLmsr = currentEvent.type() == EventType.LMSR;
        EventInfoDTO openedEvent = engine.openEvent(eventId, marketMakerName);
        onDataChanged.run();

        String paid = Formats.decimal(openedEvent.accountBalance());
        String message;
        if (isLmsr) {
            message = marketMakerName + " opened '" + eventName + "' and paid the initial subsidy of "
                    + paid + ". Trading in the event is now allowed.";
        } else {
            long pairs = findParticipant(engine.getOrderBookState(eventId), marketMakerName)
                    .sharesPerOption().get(0);
            message = marketMakerName + " opened '" + eventName + "', paid the initial investment of "
                    + paid + " and received " + pairs + " shares of every option, which may now be "
                    + "offered for sale. Trading in the event is now allowed.";
        }
        reportStatusChange("The event was opened", message);
    }

    @FXML
    private void onBuy() {
        TradeInput input = readTradeInput(BUYING_HEADER, buyOptionComboBox,
                "Please choose the option you would like to buy.", quantityField);
        if (input == null) {
            return;
        }

        String buyerName = input.userName();
        PurchaseResultDTO result = engine.buyShares(currentEvent.id(), buyerName, input.optionIndex(),
                input.quantity());
        quantityField.clear();
        onDataChanged.run();

        String details = buyerName + " bought " + result.shares() + " shares of '" + result.optionName() + "'.\n"
                + "Shares cost: " + Formats.decimal(result.sharesCost()) + "\n"
                + "Commission: " + Formats.decimal(result.commissionPaid()) + "\n"
                + "Total paid: " + Formats.decimal(result.totalPaid()) + "\n"
                + "Balance of " + buyerName + ": " + Formats.decimal(result.buyerBalance());
        showActionResult("The purchase was completed", buyerName, details, result.buyerBlocked());
    }

    @FXML
    private void onPlaceOrder() {
        TradeInput input = readTradeInput(ORDER_HEADER, orderOptionComboBox,
                "Please choose the option whose shares you would like to trade.", orderQuantityField);
        if (input == null) {
            return;
        }
        Double price = ViewUtils.readNumber(orderPriceField, Double::valueOf, ORDER_HEADER,
                text -> "The price '" + text + "' is not a number. Please enter the price per share, for "
                        + "example 0.45.");
        if (price == null) {
            return;
        }

        String userName = input.userName();
        OrderSide side = orderBuyToggle.isSelected() ? OrderSide.BUY : OrderSide.SELL;
        String optionName = currentEvent.optionNames().get(input.optionIndex());
        OrderResultDTO result = engine.submitOrder(new OrderRequestDTO(currentEvent.id(), userName, side,
                input.optionIndex(), input.quantity(), price));
        orderQuantityField.clear();
        orderPriceField.clear();
        onDataChanged.run();

        showActionResult("The order was placed", userName,
                describeOrderResult(userName, side, optionName, input.quantity(), price, result),
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
        if (!Dialogs.confirm("Closing the event '" + eventName + "'", "Close the event with '" + winnerName + "' as the winning option? The winners will be paid, and the event "
                + "cannot be traded or opened again.")) {
            return;
        }

        String marketMakerName = actingUser.name();
        engine.closeEvent(currentEvent.id(), marketMakerName, winnerIndex);
        onDataChanged.run();
        reportStatusChange("The event was closed", "'" + eventName + "' was closed by "
                + marketMakerName + " with '" + winnerName + "' as the winning option. The winners "
                + "were paid, and the commission and what was left in the event account went to "
                + "the market maker.");
    }

    /**
     * Reads what every trade needs before it is sent to the engine: the user who performs it, the
     * chosen option and a quantity that is a whole number. A trade can be started with the Enter key
     * in its field as well, even while its button is not available.
     *
     * @return the details of the trade, or {@code null} after telling the user what is missing
     */
    private TradeInput readTradeInput(String header, ComboBox<String> optionComboBox, String chooseOption,
                                      TextField quantityInput) {
        UserDetailsDTO user = actingUser;
        if (user == null) {
            Dialogs.showWarning(header, CHOOSE_USER);
            return null;
        }
        int optionIndex = optionComboBox.getSelectionModel().getSelectedIndex();
        if (optionIndex < 0) {
            Dialogs.showWarning(header, chooseOption);
            return null;
        }
        Long quantity = ViewUtils.readNumber(quantityInput, Long::valueOf, header, text -> "The quantity '" + text + "' is not a whole number. Please enter the amount of shares as a positive whole "
                + "number, for example 10.");
        return quantity == null ? null : new TradeInput(user.name(), optionIndex, quantity);
    }

    /**
     * Reports that the status of the event changed. The dialog does not block, so the status is
     * pulsed once the dialog was closed - pulsing right away would happen behind it, unseen.
     */
    private void reportStatusChange(String header, String message) {
        Dialogs.showInformation(header, message).setOnHidden(closed -> Animations.pulse(statusValue));
    }

    /**
     * Reports a completed purchase or order. When it brought the balance of the user below zero, the
     * report is a warning that the user is now blocked.
     */
    private void showActionResult(String header, String userName, String details, boolean isUserBlocked) {
        if (isUserBlocked) {
            Dialogs.showWarning(header + " - " + userName + " is now blocked", details
                    + "\n\nThe balance dropped below zero, so " + userName + " is blocked from opening "
                    + "and creating events, buying shares and placing orders from now on.");
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
                + side.getDisplayName().toLowerCase(Locale.ROOT) + " " + quantity + " shares of '" + optionName + "' at " + Formats.decimal(price) + ".\n");

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

    /**
     * @return the name of the user who performs the actions here, or {@code null} when none is
     *         chosen. Used to start the form of a new event with the user already at hand.
     */
    public String actingUserName() {
        UserDetailsDTO user = actingUser;
        return user == null ? null : user.name();
    }

    private void updateActions() {
        if (currentEvent == null) {
            return;
        }
        UserDetailsDTO user = actingUser;
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

    private String describeAvailableActions(UserDetailsDTO user, EventStatus status, boolean isMarketMaker,
                                            boolean isBlocked) {
        if (status == EventStatus.CLOSED) {
            return "This event is closed - no further actions are possible.";
        }
        if (user == null) {
            return CHOOSE_USER;
        }
        if (isBlocked) {
            return user.name() + " is blocked (the balance dropped below zero) and cannot open or create "
                    + "events, buy shares or place orders." + (isMarketMaker && status == EventStatus.ACTIVE
                    ? " As the market maker, " + user.name() + " may still close this event."
                    : "");
        }
        if (status == EventStatus.INACTIVE && !isMarketMaker) {
            return "This event is not open yet. Only its market maker '" + currentEvent.marketMakerName() + "' can open it.";
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
            optionBookViews.get(index).show(state.options().get(index));
        }

        rebuildParticipantColumns(state.eventInfo().optionNames());
        ViewUtils.replaceItems(participantsTable, state.participants());
        ViewUtils.replaceItems(orderBookTradesTable, state.tradeHistory());
    }

    /**
     * The participants table has two columns (shares and value) for every option, named after the
     * options of the shown event. The columns are replaced only for options with other names, so the
     * widths the user gave them stay while the same event is refreshed.
     */
    private void rebuildParticipantColumns(List<String> optionNames) {
        List<TableColumn<OrderBookParticipantDTO, String>> columns = new ArrayList<>();
        columns.add(ViewUtils.column("User", OrderBookParticipantDTO::userName));
        for (int index = 0; index < optionNames.size(); index++) {
            int optionIndex = index;
            columns.add(ViewUtils.numericColumn(optionNames.get(index) + " shares",
                    participant -> String.valueOf(participant.sharesPerOption().get(optionIndex))));
            columns.add(ViewUtils.numericColumn(optionNames.get(index) + " value",
                    participant -> Formats.optionalDecimal(participant.holdingValuePerOption().get(optionIndex))));
        }
        if (!titlesOf(columns).equals(titlesOf(participantsTable.getColumns()))) {
            participantsTable.getColumns().setAll(columns);
        }
    }

    private static List<String> titlesOf(List<? extends TableColumn<?, ?>> columns) {
        return columns.stream().map(TableColumn::getText).toList();
    }

    /**
     * Shows what the acting user holds, paid and received in the shown order book event, when the
     * acting user takes part in it.
     */
    private void showPosition(UserDetailsDTO user) {
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
            addPositionRow(row++, optionNames.get(index),
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
        positionGrid.add(ViewUtils.fieldName(name), 0, row);
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
        // Refilling a choice box clears its selection, so it is refilled only for a different event.
        if (!buyOptionComboBox.getItems().equals(optionNames)) {
            buyOptionComboBox.getItems().setAll(optionNames);
            orderOptionComboBox.getItems().setAll(optionNames);
            winnerComboBox.getItems().setAll(optionNames);
        }
    }

    private void showSummary(EventStateDTO state) {
        eventNameLabel.setText(currentEvent.name());
        descriptionLabel.setText(currentEvent.description());
        statusValue.setText(currentEvent.status().getDisplayName());
        typeValue.setText(currentEvent.type().getDisplayName());
        commissionValue.setText(
                Formats.commission(currentEvent.commissionPercent(), currentEvent.commissionType()));
        marketMakerValue.setText(currentEvent.marketMakerName());
        balanceValue.setText(Formats.decimal(currentEvent.accountBalance()));
        commissionCollectedValue.setText(Formats.decimal(state.totalCommissionCollected()));
        winnerValue.setText(state.winningOption().orElse(Formats.NOT_AVAILABLE));
    }

    /**
     * The details every trade starts with.
     */
    private record TradeInput(String userName, int optionIndex, long quantity) {
    }
}
