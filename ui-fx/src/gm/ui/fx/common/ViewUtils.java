package gm.ui.fx.common;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Small helpers shared by the controllers of the screens.
 */
public final class ViewUtils {

    private static final String FIELD_NAME_STYLE = "field-name";
    private static final String NUMERIC_STYLE = "numeric";

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

    /**
     * Creates a column whose text is taken out of every row, for a table that is built in code.
     */
    public static <S> TableColumn<S, String> column(String title, Function<S, String> text) {
        TableColumn<S, String> column = new TableColumn<>(title);
        bindText(column, text);
        return column;
    }

    /**
     * Like {@link #column}, for a column of numbers.
     */
    public static <S> TableColumn<S, String> numericColumn(String title, Function<S, String> text) {
        TableColumn<S, String> column = column(title, text);
        column.getStyleClass().add(NUMERIC_STYLE);
        return column;
    }

    /**
     * Creates the label that names a value, for a screen part that is built in code.
     */
    public static Label fieldName(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(FIELD_NAME_STYLE);
        return label;
    }

    /**
     * Selects the first row of the table that matches, when there is one. The rows are new objects
     * after every refresh, so a row that was selected is found again by what it describes.
     */
    public static <S> void selectFirst(TableView<S> table, Predicate<S> matches) {
        for (S row : table.getItems()) {
            if (matches.test(row)) {
                table.getSelectionModel().select(row);
                return;
            }
        }
    }

    /**
     * Replaces the rows of the table. The row that was selected stays selected when a row that
     * describes the same thing is still there.
     *
     * @param isSameRow tells whether a new row (first) describes the same thing as the selected one
     */
    public static <S> void replaceItems(TableView<S> table, List<S> items, BiPredicate<S, S> isSameRow) {
        S selected = table.getSelectionModel().getSelectedItem();
        table.getSelectionModel().clearSelection();
        table.getItems().setAll(items);
        if (selected != null) {
            selectFirst(table, row -> isSameRow.test(row, selected));
        }
    }

    /**
     * Reads a number the user typed into a field.
     *
     * @param parser         converts the text, and throws a {@link NumberFormatException} when it is
     *                       not such a number
     * @param invalidMessage the warning for a text that is not such a number, given that text
     * @return the number, or {@code null} after warning the user
     */
    public static <T> T readNumber(TextField field, Function<String, T> parser, String header,
                                   Function<String, String> invalidMessage) {
        String text = field.getText().trim();
        try {
            return parser.apply(text);
        } catch (NumberFormatException notANumber) {
            Dialogs.showWarning(header, invalidMessage.apply(text));
            field.requestFocus();
            return null;
        }
    }
}
