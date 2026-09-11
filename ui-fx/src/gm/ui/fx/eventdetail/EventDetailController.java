package gm.ui.fx.eventdetail;

import gm.dto.EventInfoDTO;
import gm.dto.EventType;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.TradeRecordDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Formats;
import gm.ui.fx.common.ViewUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/**
 * The details of a single event. Used by both screens: the events screen shows the selected event,
 * and the users screen shows the selected event of the selected user.
 */
public class EventDetailController {

    private static final String NOT_AVAILABLE = "-";

    @FXML private Label placeholderLabel;
    @FXML private VBox detailsBox;
    @FXML private Label eventNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusValue;
    @FXML private Label typeValue;
    @FXML private Label commissionValue;
    @FXML private Label marketMakerValue;
    @FXML private Label balanceValue;
    @FXML private Label commissionCollectedValue;
    @FXML private Label winnerValue;
    @FXML private VBox lmsrBox;
    @FXML private TableView<OptionStateDTO> optionsTable;
    @FXML private TableColumn<OptionStateDTO, String> optionNumberColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionNameColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionValueColumn;
    @FXML private TableColumn<OptionStateDTO, String> optionSharesColumn;
    @FXML private TableView<TradeRecordDTO> historyTable;
    @FXML private TableColumn<TradeRecordDTO, String> buyerColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeOptionColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeSharesColumn;
    @FXML private TableColumn<TradeRecordDTO, String> sharesCostColumn;
    @FXML private TableColumn<TradeRecordDTO, String> tradeCommissionColumn;
    @FXML private TableColumn<TradeRecordDTO, String> totalPaidColumn;
    @FXML private Label orderBookNoteLabel;

    private GuessMarketEngine engine;

    @FXML
    private void initialize() {
        // Options are presented to the user starting from 1.
        ViewUtils.bindText(optionNumberColumn, option -> "");
        optionNumberColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        ViewUtils.bindText(optionNameColumn, OptionStateDTO::name);
        ViewUtils.bindText(optionValueColumn, option -> Formats.decimal(option.value()));
        ViewUtils.bindText(optionSharesColumn, option -> String.valueOf(option.shares()));

        ViewUtils.bindText(buyerColumn, TradeRecordDTO::buyerName);
        ViewUtils.bindText(tradeOptionColumn, TradeRecordDTO::optionName);
        ViewUtils.bindText(tradeSharesColumn, trade -> String.valueOf(trade.shares()));
        ViewUtils.bindText(sharesCostColumn, trade -> Formats.decimal(trade.sharesCost()));
        ViewUtils.bindText(tradeCommissionColumn, trade -> Formats.decimal(trade.commissionPaid()));
        ViewUtils.bindText(totalPaidColumn, trade -> Formats.decimal(trade.totalPaid()));

        clear();
    }

    public void setEngine(GuessMarketEngine engine) {
        this.engine = engine;
    }

    /**
     * Shows the current state of an event, pulled from the engine.
     */
    public void showEvent(EventInfoDTO event) {
        ViewUtils.show(placeholderLabel, false);
        ViewUtils.show(detailsBox, true);

        boolean isLmsr = event.type() == EventType.LMSR;
        if (isLmsr) {
            MarketStateDTO state = engine.getMarketState(event.id());
            showSummary(state.eventInfo(), Formats.decimal(state.totalCommissionCollected()),
                    state.winningOption().orElse(NOT_AVAILABLE));
            optionsTable.getItems().setAll(state.optionStates());
            historyTable.getItems().setAll(state.tradeHistory());
        } else {
            showSummary(event, NOT_AVAILABLE, NOT_AVAILABLE);
        }
        ViewUtils.show(lmsrBox, isLmsr);
        ViewUtils.show(orderBookNoteLabel, !isLmsr);
    }

    public void clear() {
        ViewUtils.show(placeholderLabel, true);
        ViewUtils.show(detailsBox, false);
        optionsTable.getItems().clear();
        historyTable.getItems().clear();
    }

    private void showSummary(EventInfoDTO event, String commissionCollected, String winner) {
        eventNameLabel.setText(event.name() + " (id " + event.id() + ")");
        descriptionLabel.setText(event.description());
        statusValue.setText(event.status().getDisplayName());
        typeValue.setText(event.type().getDisplayName());
        commissionValue.setText(Formats.commission(event.commissionPercent(), event.commissionType()));
        marketMakerValue.setText(event.marketMakerName());
        balanceValue.setText(Formats.decimal(event.accountBalance()));
        commissionCollectedValue.setText(commissionCollected);
        winnerValue.setText(winner);
    }
}
