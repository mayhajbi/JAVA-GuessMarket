package gm.ui.fx.eventdetail;

import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderDTO;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * The order book of a single option: its price statistics and the waiting buy and sell orders.
 */
class OptionBookView extends VBox {

    private static final String[] STATISTICS = {"LAST", "BID", "ASK", "MID", "SPREAD"};
    private static final double TABLE_HEIGHT = 130;
    private static final double TABLE_MIN_HEIGHT = 90;

    private final Label titleLabel = new Label();
    private final Label[] statisticValues = new Label[STATISTICS.length];
    private final TableView<OrderDTO> bidsTable = createOrdersTable("No buy orders.");
    private final TableView<OrderDTO> asksTable = createOrdersTable("No sell orders.");

    OptionBookView() {
        setSpacing(6);
        HBox.setHgrow(this, Priority.ALWAYS);
        titleLabel.getStyleClass().add("section-title");

        GridPane statistics = new GridPane();
        statistics.setHgap(14);
        for (int index = 0; index < STATISTICS.length; index++) {
            statisticValues[index] = new Label("-");
            statistics.add(fieldName(STATISTICS[index]), index, 0);
            statistics.add(statisticValues[index], index, 1);
        }

        getChildren().addAll(titleLabel, statistics, fieldName("Bids (buy)"), bidsTable,
                fieldName("Asks (sell)"), asksTable);
    }

    void show(int optionNumber, OrderBookOptionDTO option) {
        titleLabel.setText(optionNumber + ". " + option.optionName());
        Double[] values = {option.lastPrice(), option.bestBid(), option.bestAsk(), option.midPrice(),
                option.spread()};
        for (int index = 0; index < values.length; index++) {
            statisticValues[index].setText(Formats.optionalDecimal(values[index]));
        }
        bidsTable.getItems().setAll(option.bids());
        asksTable.getItems().setAll(option.asks());
    }

    private static Label fieldName(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-name");
        return label;
    }

    private static TableView<OrderDTO> createOrdersTable(String emptyText) {
        TableView<OrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(TABLE_HEIGHT);
        table.setMinHeight(TABLE_MIN_HEIGHT);
        Label placeholder = new Label(emptyText);
        placeholder.getStyleClass().add("placeholder");
        table.setPlaceholder(placeholder);

        TableColumn<OrderDTO, String> userColumn = new TableColumn<>("User");
        TableColumn<OrderDTO, String> quantityColumn = new TableColumn<>("Quantity");
        TableColumn<OrderDTO, String> priceColumn = new TableColumn<>("Price");
        ViewUtils.bindText(userColumn, OrderDTO::userName);
        ViewUtils.bindText(quantityColumn, order -> String.valueOf(order.quantity()));
        ViewUtils.bindText(priceColumn, order -> Formats.decimal(order.price()));
        quantityColumn.getStyleClass().add("numeric");
        priceColumn.getStyleClass().add("numeric");
        table.getColumns().setAll(List.of(userColumn, quantityColumn, priceColumn));
        return table;
    }
}
