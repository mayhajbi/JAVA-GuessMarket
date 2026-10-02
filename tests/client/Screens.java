import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * What the checks of the client share when they use a screen the way a user does: reaching a part of a
 * screen that is private to its controller, pressing Enter, and reading the dialogs an action opened.
 */
final class Screens {

    private Screens() {
    }

    /**
     * @return the part the controller keeps in the field of this name
     */
    @SuppressWarnings("unchecked")
    static <T> T part(Object controller, String fieldName) throws ReflectiveOperationException {
        Field field = controller.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(controller);
    }

    /** What pressing Enter in a field, or clicking a button, does. */
    static void fire(Node node) {
        node.fireEvent(new ActionEvent());
    }

    /**
     * Closes the dialogs that are open now.
     *
     * @return the header and the message of every dialog that was open, as "header: message"
     */
    static List<String> closeDialogs() {
        // Without this, closing the only window that is open would shut the JavaFX thread down.
        Platform.setImplicitExit(false);
        List<String> dialogs = new ArrayList<>();
        for (Window window : List.copyOf(Window.getWindows())) {
            if (window.getScene() != null && window.getScene().getRoot() instanceof DialogPane pane) {
                dialogs.add(pane.getHeaderText() + ": " + pane.getContentText());
                window.hide();
            }
        }
        return dialogs;
    }

    /**
     * Answers the dialog that the next action opens and waits for: the answer is given from the loop that
     * the dialog starts, once it is showing, by pressing the button of the given type.
     *
     * @param seenText receives the text of the dialog, as the user read it
     */
    static void answerNextDialog(ButtonType answer, List<String> seenText) {
        Platform.runLater(() -> {
            for (Window window : List.copyOf(Window.getWindows())) {
                if (window.getScene() != null && window.getScene().getRoot() instanceof DialogPane pane) {
                    seenText.add(pane.getHeaderText() + ": " + pane.getContentText());
                    ((Button) pane.lookupButton(answer)).fire();
                }
            }
        });
    }
}
