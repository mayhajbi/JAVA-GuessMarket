package gm.ui.fx.common;

import gm.engine.exception.GuessMarketException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

/**
 * The dialogs the application uses to report the result of an action to the user.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Reports a failure. The message of the engine is shown as is, so this method does not need to
     * know the specific type of the failure. An exception thrown from an action handler arrives
     * wrapped by JavaFX, so the chain of causes is searched for the original engine exception.
     *
     * @param header short description of the action that failed
     */
    public static void showError(String header, Throwable failure) {
        Throwable cause = originalCause(failure);
        if (!(cause instanceof GuessMarketException)) {
            failure.printStackTrace();
        }
        String message = cause.getMessage() != null ? cause.getMessage() : cause.toString();
        show(Alert.AlertType.ERROR, header, message);
    }

    /**
     * Reports an action that was completed successfully.
     */
    public static void showInformation(String header, String message) {
        show(Alert.AlertType.INFORMATION, header, message);
    }

    private static void show(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.show();
    }

    private static Throwable originalCause(Throwable failure) {
        Throwable current = failure;
        while (!(current instanceof GuessMarketException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
