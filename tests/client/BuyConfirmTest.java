import gm.dto.CommissionType;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserDetailsDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.eventdetail.EventDetailController;
import gm.ui.fx.events.EventsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Buying shares asks first: the dialog shows the price, the commission and the balance that is left, and
 * Cancel buys nothing and keeps what was typed, while OK buys. An order to buy asks the same with the
 * highest it can cost. Needs nothing running: the screen works here against a canned engine that records
 * what it was asked to do.
 */
public class BuyConfirmTest extends Check {

    static final List<String> OPTIONS = List.of("Yes", "No");
    static final UserDetailsDTO USER = new UserDetailsDTO("Dana", 100, false, List.of());
    static final EventInfoDTO LMSR = new EventInfoDTO(1, "Event 1", "A canned LMSR event", 5,
            CommissionType.ON_PURCHASE, OPTIONS, EventStatus.ACTIVE, EventType.LMSR, "Mor", 100);
    static final EventInfoDTO BOOK = new EventInfoDTO(2, "Event 2", "A canned order book event", 5,
            CommissionType.ON_PURCHASE, OPTIONS, EventStatus.ACTIVE, EventType.ORDER_BOOK, "Mor", 100);
    static final PurchaseResultDTO QUOTE = new PurchaseResultDTO("Yes", 10, 5, 0.25, 5.25, 94.75, false);

    /** What the engine was asked to do, in order. */
    static final List<String> calls = new ArrayList<>();

    public static void main(String[] args) {
        run("buy-confirm", () -> FxThread.run(BuyConfirmTest::check));
    }

    static void check() throws Exception {
        FXMLLoader loader = new FXMLLoader(EventsController.class.getResource("/gm/ui/fx/events/events.fxml"));
        loader.load();
        EventsController screen = loader.getController();
        @SuppressWarnings("unchecked")
        TableView<EventInfoDTO> eventsTable = (TableView<EventInfoDTO>) loader.getNamespace().get("eventsTable");
        EventDetailController detail = screen.eventDetail();
        screen.setEngine(engine());
        screen.setUserName(USER.name());
        screen.refresh();
        screen.showActingUser(USER);
        Screens.closeDialogs();

        // LMSR
        eventsTable.getSelectionModel().select(0);
        TextField quantity = Screens.part(detail, "quantityField");
        ComboBox<String> option = Screens.part(detail, "buyOptionComboBox");
        Button buy = Screens.part(detail, "buyButton");
        quantity.setText("10");
        option.getSelectionModel().select("Yes");

        List<String> seen = new ArrayList<>();
        calls.clear();
        Screens.answerNextDialog(ButtonType.CANCEL, seen);
        buy.fire();
        expect("[quoteShares]", calls.toString(), "Cancel buys nothing");
        expect("10", quantity.getText(), "Cancel keeps what was typed");
        expect(1, seen.size(), "the user is asked once");
        expectTrue(seen.get(0).startsWith("Confirm purchase: Buy 10 shares of 'Yes'?")
                && seen.get(0).contains("Total: 5.25") && seen.get(0).contains("Balance after: 94.75"),
                "the question shows the price and the balance that is left: " + seen.get(0));

        calls.clear();
        Screens.answerNextDialog(ButtonType.OK, new ArrayList<>());
        buy.fire();
        expect("[quoteShares, buyShares]", calls.toString(), "OK buys");
        expect("", quantity.getText(), "the quantity is cleared once the shares are bought");
        Screens.closeDialogs();

        // order book: only an order to buy asks
        eventsTable.getSelectionModel().select(1);
        TextField orderQuantity = Screens.part(detail, "orderQuantityField");
        TextField orderPrice = Screens.part(detail, "orderPriceField");
        ComboBox<String> orderOption = Screens.part(detail, "orderOptionComboBox");
        Button placeOrder = Screens.part(detail, "placeOrderButton");
        orderQuantity.setText("4");
        orderPrice.setText("0.33");
        orderOption.getSelectionModel().select("Yes");

        seen.clear();
        calls.clear();
        Screens.answerNextDialog(ButtonType.CANCEL, seen);
        placeOrder.fire();
        expect("[]", calls.toString(), "Cancel places no order");
        expect("4", orderQuantity.getText(), "Cancel keeps the quantity of the order");
        expectTrue(seen.get(0).contains("Highest cost: 1.32") && seen.get(0).contains("Balance after: 98.61"),
                "the question shows the highest cost and the balance that is left: " + seen.get(0));
    }

    /** An engine that answers what the screen asks and records what it is asked to buy. */
    static GuessMarketEngine engine() {
        List<EventInfoDTO> events = List.of(LMSR, BOOK);
        return (GuessMarketEngine) Proxy.newProxyInstance(BuyConfirmTest.class.getClassLoader(),
                new Class<?>[] {GuessMarketEngine.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "quoteShares", "buyShares", "submitOrder" -> calls.add(method.getName());
                        default -> { }
                    }
                    return switch (method.getName()) {
                        case "getUserDetails" -> USER;
                        case "getAllEvents", "getEvents" -> events;
                        case "getMarketState" -> new MarketStateDTO(LMSR, List.of(new OptionStateDTO("Yes", 0.5, 3),
                                new OptionStateDTO("No", 0.5, 0)), 100, 0, List.of(), null);
                        case "getOrderBookState" -> new OrderBookStateDTO(BOOK, 1, true, 100,
                                List.of(new OrderBookOptionDTO("Yes", List.of(), List.of(), null, null, null, null, null),
                                        new OrderBookOptionDTO("No", List.of(), List.of(), null, null, null, null, null)),
                                List.of(), List.of(), 0, null);
                        case "getEventPriceHistory" -> List.of();
                        case "quoteShares", "buyShares" -> QUOTE;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }
}
