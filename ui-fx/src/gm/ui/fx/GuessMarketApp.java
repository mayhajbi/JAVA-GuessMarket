package gm.ui.fx;

import gm.engine.api.GuessMarketEngine;
import gm.engine.impl.GuessMarketEngineImpl;
import gm.ui.fx.app.AppController;
import gm.ui.fx.common.Dialogs;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The JavaFX application of exercise 2.
 * <p>
 * This is the single place in the system where the concrete engine implementation is created; every
 * other class in this module works against the {@link GuessMarketEngine} interface only.
 */
public class GuessMarketApp extends Application {

    private static final String APP_FXML = "app/app.fxml";
    private static final String TITLE = "Guess Market";
    private static final double INITIAL_WIDTH = 1200;
    private static final double INITIAL_HEIGHT = 760;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(APP_FXML));
        Parent root = loader.load();
        AppController appController = loader.getController();
        appController.setEngine(new GuessMarketEngineImpl());

        // The single central point that reports failures: any exception that escapes an action
        // handler is shown to the user with its message, whatever its specific type is.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, failure) -> Dialogs.showError("The action could not be completed", failure));

        stage.setScene(new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT));
        stage.setTitle(TITLE);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
