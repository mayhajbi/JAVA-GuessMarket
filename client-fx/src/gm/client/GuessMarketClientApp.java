package gm.client;

import gm.client.app.AppController;
import gm.client.login.LoginController;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * The JavaFX application of exercise 3: the login screen, and after it the main window.
 * <p>
 * This is the single place in the system where the concrete engine of the client is created; every other
 * class works against the {@link GuessMarketEngine} interface only.
 */
public class GuessMarketClientApp extends Application {

    private static final String LOGIN_FXML = "login/login.fxml";
    private static final String APP_FXML = "app/app.fxml";
    private static final String TITLE = "Guess Market";
    private static final double INITIAL_WIDTH = 1200;
    private static final double INITIAL_HEIGHT = 760;

    private final HttpApi api = new HttpApi();
    private final GuessMarketEngine engine = new HttpGuessMarketEngine(api);
    /** The controller of the main window; {@code null} until a user logged in. */
    private AppController appController;

    @Override
    public void start(Stage stage) {
        // The single central point that reports failures: any exception that escapes an action
        // handler is shown to the user with its message, whatever its specific type is.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, failure) -> Dialogs.showError("The action could not be completed", failure));

        showLogin(stage);
        stage.show();
    }

    private void showLogin(Stage stage) {
        FXMLLoader loader = load(LOGIN_FXML);
        LoginController loginController = loader.getController();
        loginController.start(engine, userName -> showMainWindow(stage, userName));

        stage.setScene(new Scene(loader.getRoot()));
        stage.setTitle(TITLE);
        stage.sizeToScene();
        stage.centerOnScreen();
    }

    private void showMainWindow(Stage stage, String userName) {
        FXMLLoader loader = load(APP_FXML);
        appController = loader.getController();
        appController.start(engine, api, userName, () -> loggedOut(stage));
        stage.setScene(new Scene(loader.getRoot(), INITIAL_WIDTH, INITIAL_HEIGHT));
        stage.setTitle(TITLE + " - " + userName);
        stage.centerOnScreen();
    }

    /**
     * The server does not know the user any more, which happens when it was restarted: a server that
     * starts again has no users. The user is sent back to the login screen and told why.
     */
    private void loggedOut(Stage stage) {
        appController = null;
        showLogin(stage);
        Dialogs.showWarning("You were logged out", "The server was restarted and no longer has any users or "
                + "events. Please log in again.");
    }

    private FXMLLoader load(String fxml) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        try {
            loader.load();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return loader;
    }

    /**
     * Called when the window is closed: the timers of the automatic updates and the threads of the HTTP
     * client are stopped, so that the application really exits.
     */
    @Override
    public void stop() {
        if (appController != null) {
            appController.close();
        }
        api.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}