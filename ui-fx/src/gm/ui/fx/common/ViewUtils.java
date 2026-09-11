package gm.ui.fx.common;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/**
 * Small helpers shared by the controllers of the screens.
 */
public final class ViewUtils {

    private ViewUtils() {
    }

    /**
     * Shows or hides a node. A hidden node also gives up its place in the layout.
     */
    public static void show(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    /**
     * Fills a text column out of every row object. The rows are immutable data transfer objects, so
     * the text is simply taken once for every cell.
     */
    public static <S> void bindText(TableColumn<S, String> column, Function<S, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }
}
