import gm.client.ServerException;
import gm.client.login.LoginController;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * The login screen: Enter in the field logs in like the button, and a name the server refuses is reported
 * in a dialog and stays in the field, so that it is easy to correct. Needs nothing running: the screen
 * works here against a canned engine.
 */
public class LoginViewTest extends Check {

    static final String TAKEN = "Taken";
    static final String REFUSAL = "The name 'Taken' is already in use.";

    public static void main(String[] args) {
        run("login-view", () -> FxThread.run(LoginViewTest::loginScreen));
    }

    static void loginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(LoginController.class.getResource("login.fxml"));
        Pane root = loader.load();
        LoginController login = loader.getController();
        TextField field = (TextField) loader.getNamespace().get("userNameField");
        Button button = (Button) root.lookup(".button");
        List<String> loggedIn = new ArrayList<>();
        login.start(cannedEngine(), loggedIn::add);

        field.setText("Dana");
        Screens.fire(field);
        expect("[Dana]", loggedIn.toString(), "Enter in the field logs the user in");

        field.setText("Avi");
        button.fire();
        expect("[Dana, Avi]", loggedIn.toString(), "the button logs the user in");
        expect("[]", Screens.closeDialogs().toString(), "a login that succeeded opens no dialog");

        field.setText(TAKEN);
        Screens.fire(field);
        expect("[Dana, Avi]", loggedIn.toString(), "a refused name does not log in");
        expect(TAKEN, field.getText(), "a refused name stays in the field");
        expect("[The login failed: " + REFUSAL + "]", Screens.closeDialogs().toString(),
                "a refused name is reported with the reason of the server");
    }

    /** An engine that registers every name but one. */
    static GuessMarketEngine cannedEngine() {
        return (GuessMarketEngine) Proxy.newProxyInstance(LoginViewTest.class.getClassLoader(),
                new Class<?>[] {GuessMarketEngine.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("registerUser")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    if (TAKEN.equals(args[0])) {
                        throw new ServerException(REFUSAL);
                    }
                    return new UserInfoDTO((String) args[0], 0, false, false);
                });
    }
}
