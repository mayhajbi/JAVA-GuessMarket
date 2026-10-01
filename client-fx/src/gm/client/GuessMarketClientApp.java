package gm.client;

import gm.client.app.AppController;
import gm.client.login.LoginController;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    private final GuessMarketEngine engine = new HttpGuessMarketEngine();

    @Override
    public void start(Stage stage) throws IOException {
        // The single central point that reports failures: any exception that escapes an action
        // handler is shown to the user with its message, whatever its specific type is.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, failure) -> Dialogs.showError("The action could not be completed", failure));

        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();
        loginController.start(engine, userName -> showMainWindow(stage, userName));

        stage.setScene(new Scene(loginRoot));
        stage.setTitle(TITLE);
        stage.show();
    }

    private void showMainWindow(Stage stage, String userName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(APP_FXML));
            Parent root = loader.load();
            AppController appController = loader.getController();
            appController.start(engine, userName);
            stage.setScene(new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT));
            stage.setTitle(TITLE + " - " + userName);
            stage.centerOnScreen();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}