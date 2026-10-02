import gm.dto.CommissionType;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.UserDetailsDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.HistoryChart;
import gm.ui.fx.common.ViewUtils;
import gm.ui.fx.eventdetail.EventDetailController;
import gm.ui.fx.events.EventsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * What an automatic update may and may not change on the screen: a refresh that brought nothing new
 * leaves a table and a graph untouched, the selected row stays selected, and what the user typed into
 * the quantity field of an event, or typed and chose for an order, survives every refresh of the events
 * and of the event itself.
 * Needs nothing running: the screens work here against a canned engine.
 */
public class ViewRefreshTest extends Check {

    static final List<String> OPTIONS = List.of("Yes", "No");
    static final UserDetailsDTO USER = new UserDetailsDTO("Dana", 100, false, List.of());
    static final EventInfoDTO BOOK = new EventInfoDTO(3, "Event 3", "A canned order book event", 5,
            CommissionType.ON_PURCHASE, OPTIONS, EventStatus.ACTIVE, EventType.ORDER_BOOK, "Mor", 100);

    public static void main(String[] args) {
        run("view-refresh", ViewRefreshTest::check);
    }

    static void check() throws Exception {
        FxThread.run(() -> {
            tableRows();
            chart();
            eventsScreen();
            orderFields();
        });
    }

    static void tableRows() {
        TableView<String> table = new TableView<>();
        table.getItems().setAll("a", "b", "c");
        table.getSelectionModel().select("b");
        int[] selectionChanges = {0};
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> selectionChanges[0]++);

        ViewUtils.replaceItems(table, List.of("a", "b", "c"));
        expect(0, selectionChanges[0], "equal rows do not touch the selection");
        expect("b", table.getSelectionModel().getSelectedItem(), "equal rows keep the selected row");

        ViewUtils.replaceItems(table, List.of("a", "b", "d"));
        expect("[a, b, d]", table.getItems().toString(), "changed rows are shown");
        expect("b", table.getSelectionModel().getSelectedItem(), "a row that is still there stays selected");

        ViewUtils.replaceItems(table, List.of("a"));
        expect(null, table.getSelectionModel().getSelectedItem(), "a row that is gone is not selected");
    }

    static void chart() {
        LineChart<Number, Number> chart = new LineChart<>(new NumberAxis(), new NumberAxis());
        List<HistoryPointDTO> points = List.of(new HistoryPointDTO(1000, 5), new HistoryPointDTO(3000, 7));

        expectTrue(HistoryChart.fill(chart, Map.of("Balance", points)), "a chart with points has something to draw");
        Object firstSeries = chart.getData().get(0);
        expectTrue(HistoryChart.fill(chart, Map.of("Balance", List.copyOf(points))), "the same points again");
        expectTrue(firstSeries == chart.getData().get(0), "equal points do not draw the chart again");

        HistoryChart.fill(chart, Map.of("Balance", List.of(new HistoryPointDTO(1000, 5))));
        expectTrue(firstSeries != chart.getData().get(0), "changed points draw the chart again");
        expect(1, chart.getData().get(0).getData().size(), "the chart shows the new points");

        expectFalse(HistoryChart.fill(chart, Map.of()), "a chart without points has nothing to draw");
        expect(0, chart.getData().size(), "a chart without points is empty");
    }

    static void eventsScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(EventsController.class.getResource("/gm/ui/fx/events/events.fxml"));
        loader.load();
        EventsController screen = loader.getController();
        @SuppressWarnings("unchecked")
        TableView<EventInfoDTO> eventsTable = (TableView<EventInfoDTO>) loader.getNamespace().get("eventsTable");
        EventDetailController detail = screen.eventDetail();
        TextField quantityField = Screens.part(detail, "quantityField");

        screen.setEngine(cannedEngine(List.of(event(1, 10), event(2, 10))));
        screen.setUserName(USER.name());
        screen.refresh();
        expect(2, eventsTable.getItems().size(), "the screen shows the events of the engine");

        eventsTable.getSelectionModel().select(0);
        expect(1, detail.shownEvent().id(), "the selected event is shown");
        quantityField.setText("7");

        screen.showEvents(List.of(event(1, 10), event(2, 10)));
        expect("7", quantityField.getText(), "equal events keep what was typed");

        screen.showEvents(List.of(event(1, 55), event(2, 10)));
        expect("7", quantityField.getText(), "changed events keep what was typed");
        expect(1, eventsTable.getSelectionModel().getSelectedItem().id(), "changed events keep the selected event");
        expect(1, detail.shownEvent().id(), "changed events keep the shown event");

        detail.showState(marketState(event(1, 55), 0.7));
        detail.showPrices(List.of());
        screen.showActingUser(new UserDetailsDTO("Dana", 40, false, List.of()));
        expect("7", quantityField.getText(), "a new state of the event and of the user keep what was typed");

        eventsTable.getSelectionModel().select(1);
        expect(2, detail.shownEvent().id(), "another event is shown once it is selected");
        expect("", quantityField.getText(), "another event starts with an empty quantity");

        screen.showEvents(List.of(event(1, 55)));
        expect(null, detail.shownEvent(), "an event that left the table is not shown any more");
    }

    /**
     * What the user typed and chose for an order - side, quantity, option and price - survives every
     * refresh of an order book event, and a blocked user is told why nothing can be placed.
     */
    static void orderFields() throws Exception {
        FXMLLoader loader = new FXMLLoader(EventsController.class.getResource("/gm/ui/fx/events/events.fxml"));
        loader.load();
        EventsController screen = loader.getController();
        @SuppressWarnings("unchecked")
        TableView<EventInfoDTO> eventsTable = (TableView<EventInfoDTO>) loader.getNamespace().get("eventsTable");
        EventDetailController detail = screen.eventDetail();
        screen.setEngine(cannedEngine(List.of(BOOK)));
        screen.setUserName(USER.name());
        screen.refresh();
        eventsTable.getSelectionModel().select(0);

        TextField quantity = Screens.part(detail, "orderQuantityField");
        TextField price = Screens.part(detail, "orderPriceField");
        RadioButton buy = Screens.part(detail, "orderBuyToggle");
        ComboBox<String> option = Screens.part(detail, "orderOptionComboBox");
        quantity.setText("4");
        price.setText("0.33");
        buy.getToggleGroup().getToggles().get(1).setSelected(true);
        option.getSelectionModel().select("No");

        detail.showState(orderBookState(BOOK, 0.55));
        screen.showEvents(List.of(BOOK));
        screen.showActingUser(new UserDetailsDTO("Dana", 40, false, List.of()));
        expect("4", quantity.getText(), "a refresh keeps the quantity of the order");
        expect("0.33", price.getText(), "a refresh keeps the price of the order");
        expectFalse(buy.isSelected(), "a refresh keeps the chosen side of the order");
        expect("No", option.getValue(), "a refresh keeps the chosen option of the order");

        screen.showActingUser(new UserDetailsDTO("Dana", -5, true, List.of()));
        Label hint = Screens.part(detail, "actionsHintLabel");
        expectTrue(hint.getText().startsWith("You are blocked because your balance is below zero")
                && hint.getText().endsWith("buy shares or place orders."), "a blocked user is told why and until when");
        Button placeOrder = Screens.part(detail, "placeOrderButton");
        expectTrue(placeOrder.isDisabled(), "a blocked user cannot place an order");
    }

    /** An engine that answers the few questions the events screen asks, always the same way. */
    static GuessMarketEngine cannedEngine(List<EventInfoDTO> events) {
        return (GuessMarketEngine) Proxy.newProxyInstance(ViewRefreshTest.class.getClassLoader(),
                new Class<?>[] {GuessMarketEngine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getUserDetails" -> USER;
                    case "getAllEvents", "getEvents" -> events;
                    case "getMarketState" -> marketState(event((int) args[0], 10), 0.5);
                    case "getOrderBookState" -> orderBookState(BOOK, 0.40);
                    case "getEventPriceHistory" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    static EventInfoDTO event(int id, double accountBalance) {
        return new EventInfoDTO(id, "Event " + id, "A canned event", 5, CommissionType.ON_PURCHASE, OPTIONS,
                EventStatus.ACTIVE, EventType.LMSR, "Mor", accountBalance);
    }

    static OrderBookStateDTO orderBookState(EventInfoDTO event, Double lastPrice) {
        return new OrderBookStateDTO(event, 1, true, 100,
                List.of(new OrderBookOptionDTO("Yes", List.of(), List.of(), lastPrice, null, null, null, null),
                        new OrderBookOptionDTO("No", List.of(), List.of(), null, null, null, null, null)),
                List.of(), List.of(), 0, null);
    }

    static MarketStateDTO marketState(EventInfoDTO event, double firstValue) {
        return new MarketStateDTO(event, List.of(new OptionStateDTO("Yes", firstValue, 3),
                new OptionStateDTO("No", 1 - firstValue, 0)), event.accountBalance(), 0, List.of(), null);
    }
}
