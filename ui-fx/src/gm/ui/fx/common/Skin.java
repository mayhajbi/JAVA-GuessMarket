package gm.ui.fx.common;

import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.List;

/**
 * The looks the application can be shown in (bonus). Every skin beyond {@link #DEFAULT} is a style
 * sheet that is loaded on top of the regular one, so it only has to say what is different: the
 * background, the look of the buttons and the font and its size for every label.
 * <p>
 * The application starts in the default look and the user switches from the header. The chosen skin
 * is remembered here, because the application opens more windows than the main one - every dialog
 * and the form of a new event carry their own scene, and they have to be dressed the same way.
 */
public enum Skin {

    DEFAULT("Default", null),
    MIDNIGHT("Midnight", "midnight.css"),
    PARCHMENT("Parchment", "parchment.css");

    private static final String STYLE_SHEET_FOLDER = "/gm/ui/fx/app/";
    private static final String BASE_STYLE_SHEET = "app.css";

    private static Skin current = DEFAULT;

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
        current = skin;
        useCurrentSkin(nodeInWindow.getScene().getRoot().getStylesheets());
    }

    /**
     * Dresses a window the application is about to open - a dialog, or the form of a new event - in
     * the skin that is currently chosen, so that it does not stand out against the main window.
     */
    public static void dress(Parent root) {
        List<String> styleSheets = root.getStylesheets();
        String baseUrl = url(BASE_STYLE_SHEET);
        if (!styleSheets.contains(baseUrl)) {
            styleSheets.add(baseUrl);
        }
        useCurrentSkin(styleSheets);
    }

    /**
     * Replaces the extra style sheet of whatever skin is in the list with the one of the current skin.
     */
    private static void useCurrentSkin(List<String> styleSheets) {
        for (Skin other : values()) {
            if (other.styleSheet != null) {
                styleSheets.remove(other.styleSheetUrl());
            }
        }
        if (current.styleSheet != null) {
            styleSheets.add(current.styleSheetUrl());
        }
    }

    private String styleSheetUrl() {
        return url(styleSheet);
    }

    private static String url(String fileName) {
        return Skin.class.getResource(STYLE_SHEET_FOLDER + fileName).toExternalForm();
    }

    /**
     * The name the user sees, which is also what a combo box of skins shows.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
