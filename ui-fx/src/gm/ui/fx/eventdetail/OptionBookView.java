package gm.ui.fx.eventdetail;

import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderDTO;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
            statisticValues[index] = new Label(Formats.NOT_AVAILABLE);
            Label name = ViewUtils.fieldName(STATISTICS[index]);
            // A column never gets narrower than its text.
            name.setMinWidth(Region.USE_PREF_SIZE);
            statisticValues[index].setMinWidth(Region.USE_PREF_SIZE);
            statistics.add(name, index, 0);
            statistics.add(statisticValues[index], index, 1);
        }

        getChildren().addAll(titleLabel, statistics, ViewUtils.fieldName("Bids: buy orders"), bidsTable,
                ViewUtils.fieldName("Asks: sell orders"), asksTable);
    }

    void show(OrderBookOptionDTO option) {
        titleLabel.setText(option.optionName());
        Double[] values = {option.lastPrice(), option.bestBid(), option.bestAsk(), option.midPrice(),
                option.spread()};
        for (int index = 0; index < values.length; index++) {
            statisticValues[index].setText(Formats.optionalDecimal(values[index]));
        }
        ViewUtils.replaceItems(bidsTable, option.bids());
        ViewUtils.replaceItems(asksTable, option.asks());
    }

    private static TableView<OrderDTO> createOrdersTable(String emptyText) {
        TableView<OrderDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        ViewUtils.keepHeadersWhole(table);
        table.setPrefHeight(TABLE_HEIGHT);
        table.setMinHeight(TABLE_MIN_HEIGHT);
        Label placeholder = new Label(emptyText);
        placeholder.getStyleClass().add("placeholder");
        placeholder.setWrapText(true);
        table.setPlaceholder(placeholder);

        table.getColumns().setAll(List.of(
                ViewUtils.column("User", OrderDTO::userName),
                ViewUtils.numericColumn("Quantity", order -> String.valueOf(order.quantity())),
                ViewUtils.numericColumn("Price", order -> Formats.decimal(order.price()))));
        return table;
    }
}
