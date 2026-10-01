package gm.client.login;

import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * The login screen: the user types a name and the server logs the user in, registering a name that is not
 * taken yet. When the server refuses the name, its message is shown in a dialog, which fits the message, and
 * what was typed stays, so that it is easy to correct.
 */
public class LoginController {

    @FXML private TextField userNameField;
    @FXML private Button loginButton;

    private GuessMarketEngine engine;
    private Consumer<String> onLoggedIn;

    /**
     * @param onLoggedIn called with the name of the user once the server logged the user in
     */
    public void start(GuessMarketEngine engine, Consumer<String> onLoggedIn) {
        this.engine = engine;
        this.onLoggedIn = onLoggedIn;
    }

    @FXML
    private void onLogin() {
        try {
            String userName = engine.registerUser(userNameField.getText()).name();
            onLoggedIn.accept(userName);
        } catch (RuntimeException refused) {
            Dialogs.showError("The login failed", refused);
        }
    }
}