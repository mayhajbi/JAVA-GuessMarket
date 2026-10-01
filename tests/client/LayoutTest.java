import gm.dto.AccountEntryDTO;
import gm.dto.AccountEntryType;
import gm.dto.CommissionType;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.HistoryPointDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderBookTradeDTO;
import gm.dto.OrderDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.TradeRecordDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserEventDTO;
import gm.dto.UserInfoDTO;
import gm.client.account.AccountController;
import gm.client.header.HeaderController;
import gm.client.login.LoginController;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.eventdetail.EventDetailScreen;
import gm.ui.fx.events.EventsController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The screens of the client in a big window and in small ones: a screen is never squeezed below the size
 * it needs (it scrolls instead), nothing sticks out of it where it would be cut, and no label or button
 * is too narrow for its text. Needs nothing running: the screens work here against a canned engine, and
 * no window is opened.
 */
public class LayoutTest extends Check {

    static final double[][] WINDOW_SIZES = {{1200, 760}, {800, 600}, {640, 480}};
    static final double TOLERANCE = 1;
    static final String USER = "Dana";
    static final List<String> OPTIONS = List.of("Yes", "No");
    static final EventInfoDTO LMSR = event(1, "Will it rain tomorrow ?", EventType.LMSR);
    static final EventInfoDTO ORDER_BOOK = event(2, "Earth Quake on Dead Sea", EventType.ORDER_BOOK);

    public static void main(String[] args) {
        run("layout", LayoutTest::check);
    }

    static void check() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                // Every screen is found next to its controller, which also compiles the controller for the check.
                screenWithEvents(EventsController.class.getResource("events.fxml"), "eventsTable", "events screen");
                screenWithEvents(AccountController.class.getResource("account.fxml"), "userEventsTable",
                        "account screen");
                plainScreen(LoginController.class.getResource("login.fxml"), "login screen");
                plainScreen(HeaderController.class.getResource("header.fxml"), "header");
                done.complete(null);
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        });
        done.get();
    }

    /** A screen that shows the details of an event: checked with an event of each trading method. */
    static void screenWithEvents(URL fxml, String eventsTableId, String name) throws Exception {
        FXMLLoader loader = new FXMLLoader(fxml);
        ScrollPane root = loader.load();
        new Scene(root).getStylesheets().add("/gm/ui/fx/common/app.css");
        EventDetailScreen screen = loader.getController();
        screen.setEngine(cannedEngine());
        screen.setUserName(USER);
        screen.getClass().getMethod("refresh").invoke(screen);
        TableView<?> eventsTable = (TableView<?>) loader.getNamespace().get(eventsTableId);

        for (int row = 0; row < eventsTable.getItems().size(); row++) {
            eventsTable.getSelectionModel().select(row);
            String shown = name + " with " + screen.eventDetail().shownEvent().type().getDisplayName();
            for (double[] size : WINDOW_SIZES) {
                String what = shown + " at " + (int) size[0] + "x" + (int) size[1];
                layOut(root, size);
                Region content = (Region) root.getContent();
                expectTrue(content.getWidth() + TOLERANCE >= content.minWidth(-1)
                        && content.getHeight() + TOLERANCE >= content.minHeight(-1),
                        what + ": the screen scrolls instead of shrinking below its minimal size");
                expectTrue(content.getWidth() + TOLERANCE >= root.getViewportBounds().getWidth(),
                        what + ": the screen fills the width of the window");
                checkParts(content, what);
            }
        }
    }

    /** A screen without events and without scrolling: it has to fit the smallest window as it is. */
    static void plainScreen(URL fxml, String name) throws Exception {
        Region root = new FXMLLoader(fxml).load();
        new Scene(root).getStylesheets().add("/gm/ui/fx/common/app.css");
        for (double[] size : WINDOW_SIZES) {
            layOut(root, new double[] {size[0], Math.max(size[1], root.minHeight(size[0]))});
            checkParts(root, name + " at " + (int) size[0] + "x" + (int) size[1]);
        }
    }

    /** Gives the screen the size of the window and lays it out, the way a shown window does. */
    static void layOut(Region root, double[] size) {
        root.resize(size[0], size[1]);
        // Twice: the first pass creates the skins of the controls, the second one lays out their content.
        for (int pass = 0; pass < 2; pass++) {
            root.applyCss();
            root.layout();
        }
    }

    static void checkParts(Region content, String what) {
        Bounds area = content.getLayoutBounds();
        for (Node part : visibleParts(content, new ArrayList<>())) {
            Bounds bounds = content.sceneToLocal(part.localToScene(part.getLayoutBounds()));
            expectTrue(bounds.getMinX() + TOLERANCE >= area.getMinX() && bounds.getMaxX() <= area.getMaxX() + TOLERANCE
                    && bounds.getMinY() + TOLERANCE >= area.getMinY() && bounds.getMaxY() <= area.getMaxY() + TOLERANCE,
                    what + ": " + describe(part) + " sticks out of the screen");
            if (part instanceof Labeled labeled && !labeled.isWrapText()) {
                expectTrue(labeled.getWidth() + TOLERANCE >= labeled.prefWidth(-1),
                        what + ": " + describe(part) + " is too narrow for its text");
            }
        }
    }

    /** The parts the layout places: what is inside the panes, not the inner parts of a control. */
    static List<Node> visibleParts(Node node, List<Node> parts) {
        List<? extends Node> children = node instanceof Pane pane ? pane.getChildren()
                : node instanceof SplitPane splitPane ? splitPane.getItems()
                : List.of();
        for (Node child : children) {
            if (child.isVisible() && child.isManaged()) {
                parts.add(child);
                visibleParts(child, parts);
            }
        }
        return parts;
    }

    static String describe(Node node) {
        String text = node instanceof Labeled labeled ? " '" + labeled.getText() + "'" : "";
        return node.getClass().getSimpleName() + text + (node.getId() == null ? "" : " #" + node.getId());
    }

    /** An engine with one user who is the market maker of an LMSR event and of an order book event. */
    static GuessMarketEngine cannedEngine() {
        List<EventInfoDTO> events = List.of(LMSR, ORDER_BOOK);
        UserDetailsDTO details = new UserDetailsDTO(USER, 1234.5, false,
                events.stream().map(event -> new UserEventDTO(event, true, true)).toList());
        List<HistoryPointDTO> points = List.of(new HistoryPointDTO(1000, 5), new HistoryPointDTO(3000, 7));
        return (GuessMarketEngine) Proxy.newProxyInstance(LayoutTest.class.getClassLoader(),
                new Class<?>[] {GuessMarketEngine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getUserDetails" -> details;
                    case "getAllEvents", "getEvents" -> events;
                    case "getAllUsers" -> List.of(new UserInfoDTO(USER, 1234.5, false, true),
                            new UserInfoDTO("Another user", 10, false, false));
                    case "getAccountEntries" -> List.of(new AccountEntryDTO(AccountEntryType.DEPOSIT, 2000, 2000),
                            new AccountEntryDTO(AccountEntryType.EVENT, -765.5, 1234.5));
                    case "getUserBalanceHistory" -> points;
                    case "getMarketState" -> new MarketStateDTO(LMSR, List.of(new OptionStateDTO("Yes", 0.7, 30),
                            new OptionStateDTO("No", 0.3, 0)), 150, 1.5,
                            List.of(new TradeRecordDTO(USER, "Yes", 30, 18.5, 0.93, 19.43)), null);
                    case "getOrderBookState" -> orderBookState();
                    case "getEventPriceHistory" -> List.of(new PriceHistoryDTO("Yes", points),
                            new PriceHistoryDTO("No", points));
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    static OrderBookStateDTO orderBookState() {
        List<OrderDTO> orders = List.of(new OrderDTO(USER, 25, 0.45));
        return new OrderBookStateDTO(ORDER_BOOK, 1, true, 1000,
                List.of(new OrderBookOptionDTO("Yes", orders, orders, 0.5, 0.45, 0.55, 0.5, 0.1),
                        new OrderBookOptionDTO("No", List.of(), List.of(), null, null, null, null, null)),
                List.of(new OrderBookParticipantDTO(USER, List.of(1000L, 975L), List.of(500.0, 487.5),
                        List.of(0.0, 0.0), 1000, 0.25, 11.25, 0)),
                List.of(new OrderBookTradeDTO("Another user", USER, "No", 25, 0.45, 0.11, false)), 0.11, null);
    }

    static EventInfoDTO event(int id, String name, EventType type) {
        return new EventInfoDTO(id, name, "A canned event with a description that is long enough to wrap in a "
                + "narrow window", 5, CommissionType.ON_PURCHASE, OPTIONS, EventStatus.ACTIVE, type, USER, 150);
    }
}
