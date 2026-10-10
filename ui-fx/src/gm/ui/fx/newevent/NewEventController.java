package gm.ui.fx.newevent;

import gm.dto.CommissionType;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;


/**
 * The form a user fills in to create a new event (bonus). It collects the details that every event
 * has, and then only the fields of the trading method that was chosen.
 * <p>
 * Whatever the user types here is checked by the engine, exactly like the content of a data file -
 * including the ranges of the numbers. The checks in this class only save the user a rejected form:
 * an empty field, or a number that is not a number at all.
 */
public class NewEventController {

    private static final String HEADER = "New event";
    private static final String EMPTY_FIELD_STYLE = "field-error";

    private String creatorName;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField firstOptionField;
    @FXML private TextField secondOptionField;
    @FXML private TextField commissionField;
    @FXML private ComboBox<CommissionType> commissionTypeComboBox;
    @FXML private RadioButton lmsrToggle;
    @FXML private VBox lmsrFields;
    @FXML private VBox orderBookFields;
    @FXML private TextField liquidityField;
    @FXML private TextField baseValueField;
    @FXML private TextField initialInvestmentField;
    @FXML private CheckBox allowMintCheckBox;

    @FXML
    private void initialize() {
        commissionTypeComboBox.getItems().setAll(CommissionType.values());
        commissionTypeComboBox.setValue(CommissionType.ON_PURCHASE);
        // The list and the chosen item show the name the user sees everywhere else, not the name of the constant.
        commissionTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CommissionType type) {
                return type == null ? "" : type.getDisplayName();
            }

            @Override
            public CommissionType fromString(String text) {
                throw new UnsupportedOperationException("The commission type is chosen from the list.");
            }
        });
        lmsrToggle.selectedProperty().addListener(
                (observable, previous, chosen) -> showMethodFields(chosen));
        showMethodFields(true);
        // A field marked as empty is unmarked as soon as the user starts to type in it.
        for (TextInputControl field : List.of(nameField, descriptionArea, firstOptionField, secondOptionField,
                commissionField, liquidityField, baseValueField, initialInvestmentField)) {
            field.textProperty().addListener((observable, previous, typed) ->
                    field.getStyleClass().remove(EMPTY_FIELD_STYLE));
        }
        // In the description, TAB moves to the next field like everywhere in the form (the text area
        // would indent); Ctrl+TAB is the move key of a text area.
        descriptionArea.addEventFilter(KeyEvent.KEY_PRESSED, pressed -> {
            if (pressed.getCode() == KeyCode.TAB && !pressed.isControlDown()) {
                pressed.consume();
                descriptionArea.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                        pressed.isShiftDown(), true, pressed.isAltDown(), pressed.isMetaDown()));
            }
        });
        descriptionArea.addEventFilter(KeyEvent.KEY_TYPED, typed -> {
            if ("\t".equals(typed.getCharacter())) {
                typed.consume();
            }
        });
    }

    /**
     * Shows who creates the event: the user who is logged in, who becomes its market maker.
     */
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    /**
     * Reads the form.
     *
     * @return the details of the event to create, or {@code null} when the form cannot be read yet -
     *         in which case the user was already told what is missing
     */
    public NewEventRequestDTO toRequest() {
        boolean isLmsr = lmsrToggle.isSelected();
        if (markEmptyFields(isLmsr)) {
            Dialogs.showWarning("Incomplete form", "Fill in all the empty fields to create a new event.");
            return null;
        }

        Integer commission = readNumber(commissionField, "the commission");
        if (commission == null) {
            return null;
        }

        int liquidity = 0;
        int baseValue = 0;
        int initialInvestment = 0;
        if (isLmsr) {
            Integer value = readNumber(liquidityField, "the liquidity value");
            if (value == null) {
                return null;
            }
            liquidity = value;
        } else {
            Integer value = readNumber(baseValueField, "the base value");
            if (value == null) {
                return null;
            }
            baseValue = value;
            Integer initial = readNumber(initialInvestmentField, "the initial investment");
            if (initial == null) {
                return null;
            }
            initialInvestment = initial;
        }

        return new NewEventRequestDTO(creatorName, nameField.getText(), descriptionArea.getText(),
                commission, commissionTypeComboBox.getValue(), firstOptionField.getText(),
                secondOptionField.getText(), isLmsr ? EventType.LMSR : EventType.ORDER_BOOK,
                liquidity, baseValue, allowMint(isLmsr), initialInvestment);
    }

    /**
     * Marks every required field that is still empty, among the fields of the chosen trading method.
     *
     * @return whether at least one field was empty
     */
    private boolean markEmptyFields(boolean isLmsr) {
        List<TextInputControl> required = new ArrayList<>(List.of(nameField, descriptionArea,
                firstOptionField, secondOptionField, commissionField));
        if (isLmsr) {
            required.add(liquidityField);
        } else {
            required.add(baseValueField);
            required.add(initialInvestmentField);
        }
        List<TextInputControl> empty = required.stream().filter(field -> field.getText().isBlank()).toList();
        for (TextInputControl field : empty) {
            if (!field.getStyleClass().contains(EMPTY_FIELD_STYLE)) {
                field.getStyleClass().add(EMPTY_FIELD_STYLE);
            }
        }
        if (!empty.isEmpty()) {
            empty.get(0).requestFocus();
        }
        return !empty.isEmpty();
    }

    private boolean allowMint(boolean isLmsr) {
        return !isLmsr && allowMintCheckBox.isSelected();
    }

    private void showMethodFields(boolean isLmsr) {
        ViewUtils.show(lmsrFields, isLmsr);
        ViewUtils.show(orderBookFields, !isLmsr);
        // The other method has other fields: the window follows the size of the form.
        if (descriptionArea.getScene() != null && descriptionArea.getScene().getWindow() != null) {
            descriptionArea.getScene().getWindow().sizeToScene();
        }
    }

    /**
     * @return the whole number in the field, or {@code null} after telling the user it is not one
     */
    private Integer readNumber(TextField field, String what) {
        return ViewUtils.readNumber(field, Integer::valueOf, HEADER, text -> "'" + text + "' is not a valid value for " + what + ". Enter a whole number, for example 10.");
    }

}
