import gm.client.Constants;
import gm.client.Refresher;
import gm.client.account.AccountController;
import gm.client.app.AppController;
import gm.client.chat.ChatController;
import gm.client.header.HeaderController;
import gm.ui.fx.events.EventsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What the main window does with the answers of the server to its automatic updates: a few updates in a
 * row without an answer show the "Server not reachable" line and lock the screens, an answer opens them
 * again, and a server that does not know the user any more sends the user back to the login screen.
 * Needs nothing running: the answers are handed to the window directly, the way a refresher does.
 */
public class ServerStatusTest extends Check {

    static final int OK = 200;
    static final int UNAUTHORIZED = 401;
    /** The controllers of the screens the main window holds: naming them here compiles them for the check. */
    static final List<Class<?>> SCREENS = List.of(HeaderController.class, EventsController.class,
            AccountController.class, ChatController.class);

    public static void main(String[] args) {
        run("server-status", ServerStatusTest::check);
    }

    static void check() throws Exception {
        FXMLLoader loader = new FXMLLoader(AppController.class.getResource("app.fxml"));
        AtomicInteger loggedOut = new AtomicInteger();
        // The window reacts to an answer on the JavaFX thread a moment later, so every step is checked
        // in the step after it.
        FxThread.run(() -> {
            loader.load();
            for (int failure = 1; failure < Constants.MAX_FAILURES; failure++) {
                answer(loader, Refresher.NO_ANSWER);
            }
        });
        Node statusLine = (Node) loader.getNamespace().get("serverStatusLabel");
        Node screens = (Node) loader.getNamespace().get("screens");
        FxThread.later(() -> {
            expectTrue(!statusLine.isVisible() && !screens.isDisabled(),
                    "fewer failures than the limit change nothing");
            answer(loader, Refresher.NO_ANSWER);
        });
        FxThread.later(() -> {
            expectTrue(statusLine.isVisible(), "the last failure in a row shows that the server is not reachable");
            expectTrue(screens.isDisabled(), "the last failure in a row locks the screens");
            answer(loader, OK);
        });
        FxThread.later(() -> {
            expectTrue(!statusLine.isVisible() && !screens.isDisabled(), "an answer opens the screens again");
            startedWith(loader, () -> loggedOut.incrementAndGet());
            answer(loader, UNAUTHORIZED);
            answer(loader, UNAUTHORIZED);
        });
        FxThread.later(() -> expect(1, loggedOut.get(),
                "a server that does not know the user sends the user to the login screen, once"));
    }

    /** Hands the window the status of one answer, the way the refresher of the account does. */
    static void answer(FXMLLoader loader, int status) throws ReflectiveOperationException {
        Method onAnswer = AppController.class.getDeclaredMethod("onAnswer", int.class);
        onAnswer.setAccessible(true);
        onAnswer.invoke(loader.getController(), status);
    }

    /**
     * Puts the window in the state it has once a user logged in - a timer runs and it knows what to do on
     * a log out - without starting requests to a server.
     */
    static void startedWith(FXMLLoader loader, Runnable onLoggedOut) throws ReflectiveOperationException {
        AppController app = loader.getController();
        Field callback = AppController.class.getDeclaredField("onLoggedOut");
        callback.setAccessible(true);
        callback.set(app, onLoggedOut);
        List<Timer> timers = Screens.part(app, "timers");
        timers.add(new Timer(true));
    }
}
