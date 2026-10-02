import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.ui.fx.newevent.NewEventController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.util.List;
import java.util.Map;

/**
 * The form of a new event: it shows the fields of the chosen trading method only, tells the user what is
 * missing, and reads what was typed. Needs nothing running, and no window is opened for the form.
 */
public class NewEventViewTest extends Check {

    public static void main(String[] args) {
        run("new-event-view", () -> FxThread.run(NewEventViewTest::form));
    }

    static void form() throws Exception {
        FXMLLoader loader = new FXMLLoader(NewEventController.class.getResource("newevent.fxml"));
        loader.load();
        NewEventController form = loader.getController();
        Map<String, Object> parts = loader.getNamespace();
        Node lmsrFields = (Node) parts.get("lmsrFields");
        Node orderBookFields = (Node) parts.get("orderBookFields");
        form.setCreatorName("Dana");

        List<String> labels = ((ScrollPane) loader.<DialogPane>getRoot().getContent()).getContent().lookupAll(".label").stream()
                .map(label -> ((Labeled) label).getText()).toList();
        expectTrue(labels.contains("Liquidity") && labels.contains("Base value"),
                "the fields of the trading methods are named in words");

        expectTrue(lmsrFields.isVisible() && !orderBookFields.isVisible(), "a new form shows the LMSR fields only");
        expect(null, form.toRequest(), "a form without a name cannot be read");
        expect("[New event: Enter a name for the event.]", Screens.closeDialogs().toString(),
                "the user is told what is missing");

        ((ToggleGroup) parts.get("methodGroup")).getToggles().get(1).setSelected(true);
        expectTrue(!lmsrFields.isVisible() && orderBookFields.isVisible(),
                "choosing Order Book shows the order book fields only");

        ((TextField) parts.get("nameField")).setText("Rain");
        ((TextArea) parts.get("descriptionArea")).setText("Will it rain");
        ((TextField) parts.get("firstOptionField")).setText("Yes");
        ((TextField) parts.get("secondOptionField")).setText("No");
        ((TextField) parts.get("commissionField")).setText("5");
        ((TextField) parts.get("baseValueField")).setText("abc");
        expect(null, form.toRequest(), "a base value that is not a number cannot be read");
        expect(1, Screens.closeDialogs().size(), "the user is told the base value is not a number");

        ((TextField) parts.get("baseValueField")).setText("1");
        ((TextField) parts.get("initialInvestmentField")).setText("100");
        NewEventRequestDTO request = form.toRequest();
        expect(List.of("Dana", "Rain", EventType.ORDER_BOOK, 1, 100, 5).toString(),
                request == null ? null : List.of(request.userName(), request.name(), request.type(),
                        request.baseValue(), request.initialInvestment(), request.commissionPercent()).toString(),
                "a full form is read as it was typed");
        expect("[]", Screens.closeDialogs().toString(), "a full form opens no dialog");
    }
}
