package gm.ui.fx.newevent;

import gm.dto.CommissionType;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.dto.UserInfoDTO;
import gm.ui.fx.common.Dialogs;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

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

    private static final String HEADER = "Creating an event";

    @FXML private ComboBox<String> userComboBox;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField firstOptionField;
    @FXML private TextField secondOptionField;
    @FXML private TextField commissionField;
    @FXML private ComboBox<CommissionType> commissionTypeComboBox;
    @FXML private RadioButton lmsrToggle;
    @FXML private RadioButton orderBookToggle;
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
        lmsrToggle.selectedProperty().addListener(
                (observable, previous, chosen) -> showMethodFields(chosen));
        showMethodFields(true);
    }

    /**
     * Offers the users of the system as the possible creator of the event.
     *
     * @param preselectedName the user to start with, when one is already selected on the screen
     */
    public void setUsers(List<UserInfoDTO> users, String preselectedName) {
        List<String> names = users.stream().map(UserInfoDTO::name).toList();
        userComboBox.getItems().setAll(names);
        if (names.contains(preselectedName)) {
            userComboBox.setValue(preselectedName);
        }
    }

    /**
     * Reads the form.
     *
     * @return the details of the event to create, or {@code null} when the form cannot be read yet -
     *         in which case the user was already told what is missing
     */
    public NewEventRequestDTO toRequest() {
        String userName = userComboBox.getValue();
        if (userName == null) {
            return missing("Please choose the user who creates the event. That user becomes its "
                    + "market maker.");
        }
        if (nameField.getText().isBlank()) {
            return missing("Please give the event a name.");
        }
        if (descriptionArea.getText().isBlank()) {
            return missing("Please describe what the event is about.");
        }
        if (firstOptionField.getText().isBlank() || secondOptionField.getText().isBlank()) {
            return missing("Please name both possible outcomes of the event.");
        }

        Integer commission = readNumber(commissionField, "the commission");
        if (commission == null) {
            return null;
        }

        boolean isLmsr = lmsrToggle.isSelected();
        int liquidity = 0;
        int baseValue = 0;
        int initialInvestment = 0;
        if (isLmsr) {
            Integer value = readNumber(liquidityField, "the liquidity value (b)");
            if (value == null) {
                return null;
            }
            liquidity = value;
        } else {
            Integer value = readNumber(baseValueField, "the base value (d)");
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

        return new NewEventRequestDTO(userName, nameField.getText(), descriptionArea.getText(),
                commission, commissionTypeComboBox.getValue(), firstOptionField.getText(),
                secondOptionField.getText(), isLmsr ? EventType.LMSR : EventType.ORDER_BOOK,
                liquidity, baseValue, allowMint(isLmsr), initialInvestment);
    }

    private boolean allowMint(boolean isLmsr) {
        return !isLmsr && allowMintCheckBox.isSelected();
    }

    private void showMethodFields(boolean isLmsr) {
        ViewUtils.show(lmsrFields, isLmsr);
        ViewUtils.show(orderBookFields, !isLmsr);
    }

    /**
     * @return the whole number in the field, or {@code null} after telling the user it is not one
     */
    private Integer readNumber(TextField field, String what) {
        String text = field.getText().trim();
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException notANumber) {
            Dialogs.showWarning(HEADER, "The value of " + what + " is [" + text
                    + "], which is not a whole number. Please enter a whole number, for example 100.");
            field.requestFocus();
            return null;
        }
    }

    private NewEventRequestDTO missing(String message) {
        Dialogs.showWarning(HEADER, message);
        return null;
    }
}
