package gm.ui.fx.events;

import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A row of toggle buttons that filters by one property: an "All" button and one button per value.
 * Every value starts selected. "All" selects or clears every value at once, and it stays selected
 * exactly when every value is selected.
 */
class FilterGroup<T> {

    private final ToggleButton allButton = new ToggleButton("All");
    private final Map<T, ToggleButton> valueButtons = new LinkedHashMap<>();

    FilterGroup(Pane container, T[] values, Function<T, String> displayName, Runnable onChange) {
        for (T value : values) {
            ToggleButton button = new ToggleButton(displayName.apply(value));
            button.setSelected(true);
            button.setOnAction(event -> {
                allButton.setSelected(areAllSelected());
                onChange.run();
            });
            valueButtons.put(value, button);
        }

        allButton.setSelected(true);
        allButton.setOnAction(event -> {
            for (ToggleButton button : valueButtons.values()) {
                button.setSelected(allButton.isSelected());
            }
            onChange.run();
        });

        container.getChildren().add(allButton);
        container.getChildren().addAll(valueButtons.values());
    }

    /**
     * @return the values whose buttons are currently selected
     */
    Set<T> selectedValues() {
        Set<T> selected = new LinkedHashSet<>();
        for (Map.Entry<T, ToggleButton> entry : valueButtons.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private boolean areAllSelected() {
        for (ToggleButton button : valueButtons.values()) {
            if (!button.isSelected()) {
                return false;
            }
        }
        return true;
    }
}
