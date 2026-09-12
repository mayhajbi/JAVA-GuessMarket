package gm.ui.fx.common;

import javafx.scene.Node;

import java.util.List;

/**
 * The looks the application can be shown in (bonus). Every skin beyond {@link #DEFAULT} is a style
 * sheet that is loaded on top of the regular one, so it only has to say what is different: the
 * background, the look of the buttons and the font and its size for every label.
 * <p>
 * The application starts in the default look and the user switches from the header.
 */
public enum Skin {

    DEFAULT("Default", null),
    MIDNIGHT("Midnight", "midnight.css"),
    PARCHMENT("Parchment", "parchment.css");

    private static final String STYLE_SHEET_FOLDER = "/gm/ui/fx/app/";

    private final String displayName;
    /** The style sheet loaded on top of the regular one; {@code null} for the default look. */
    private final String styleSheet;

    Skin(String displayName, String styleSheet) {
        this.displayName = displayName;
        this.styleSheet = styleSheet;
    }

    /**
     * Dresses the whole window in the given skin. The regular style sheet always stays in place -
     * only the extra one is replaced, so switching back and forth is safe.
     *
     * @param nodeInWindow any node that is already shown in the window
     */
    public static void apply(Node nodeInWindow, Skin skin) {
        List<String> styleSheets = nodeInWindow.getScene().getRoot().getStylesheets();
        for (Skin other : values()) {
            if (other.styleSheet != null) {
                styleSheets.remove(other.styleSheetUrl());
            }
        }
        if (skin.styleSheet != null) {
            styleSheets.add(skin.styleSheetUrl());
        }
    }

    private String styleSheetUrl() {
        return Skin.class.getResource(STYLE_SHEET_FOLDER + styleSheet).toExternalForm();
    }

    /**
     * The name the user sees, which is also what a combo box of skins shows.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
