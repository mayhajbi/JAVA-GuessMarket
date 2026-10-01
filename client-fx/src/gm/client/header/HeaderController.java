package gm.client.header;

import gm.ui.fx.common.Animations;
import gm.ui.fx.common.Skin;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

/**
 * The header of the client: the two bonus switches - the skin of the window and whether the animations
 * are played.
 */
public class HeaderController {

    @FXML private ComboBox<Skin> skinComboBox;
    @FXML private CheckBox animationsCheckBox;

    /**
     * Both bonuses start turned off, exactly as they are submitted: the regular look and no
     * animations. The user turns them on here.
     */
    @FXML
    private void initialize() {
        skinComboBox.getItems().setAll(Skin.values());
        skinComboBox.setValue(Skin.DEFAULT);
        skinComboBox.valueProperty().addListener(
                (observable, previous, chosen) -> Skin.apply(skinComboBox, chosen));
        animationsCheckBox.setSelected(false);
        animationsCheckBox.selectedProperty().addListener(
                (observable, previous, playing) -> Animations.setEnabled(playing));
    }
}