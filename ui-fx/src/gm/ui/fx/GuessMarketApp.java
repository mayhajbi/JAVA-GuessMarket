package gm.ui.fx;

import gm.engine.api.GuessMarketEngine;
import gm.engine.impl.GuessMarketEngineImpl;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * The JavaFX application of exercise 2.
 * <p>
 * This is the single place in the system where the concrete engine implementation is created; every
 * other class in this module works against the {@link GuessMarketEngine} interface only.
 * <p>
 * For now the application only opens an empty window - the screens are built in the next branches.
 */
public class GuessMarketApp extends Application {

    private final GuessMarketEngine engine = new GuessMarketEngineImpl();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane(new Label("Guess Market"));
        stage.setScene(new Scene(root, 900, 600));
        stage.setTitle("Guess Market");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
