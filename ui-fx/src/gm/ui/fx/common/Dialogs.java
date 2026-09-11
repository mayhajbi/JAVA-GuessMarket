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
        create(Alert.AlertType.ERROR, header, message).show();
    }

    /**
     * Reports an action that was completed successfully.
     */
    public static void showInformation(String header, String message) {
        create(Alert.AlertType.INFORMATION, header, message).show();
    }

    /**
     * Reports something the user has to pay attention to: an input that cannot be used, or an action
     * that was completed but has a consequence (a user that became blocked).
     */
    public static void showWarning(String header, String message) {
        create(Alert.AlertType.WARNING, header, message).show();
    }

    /**
     * Asks the user to approve an action that cannot be undone.
     *
     * @return whether the user approved
     */
    public static boolean confirm(String header, String message) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, header, message);
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private static Alert create(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(header);
        alert.setResizable(true);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alert;
    }

    private static Throwable originalCause(Throwable failure) {
        Throwable current = failure;
        while (!(current instanceof GuessMarketException) && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
