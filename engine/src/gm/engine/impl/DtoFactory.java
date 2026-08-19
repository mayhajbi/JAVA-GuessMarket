package gm.engine.impl;

import gm.dto.EventInfoDTO;
import gm.dto.MarketStateDTO;
import gm.dto.OptionStateDTO;
import gm.dto.TradeRecordDTO;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.Trade;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the data transfer objects the engine returns out of its core objects.
 * <p>
 * A new object is created on every request, so a caller never holds a reference to anything that
 * belongs to the engine.
 */
class DtoFactory {

    EventInfoDTO toEventInfo(Event event) {
        List<String> optionNames = new ArrayList<>();
        for (EventOption option : event.getOptions()) {
            optionNames.add(option.getName());
        }
        return new EventInfoDTO(event.getId(), event.getName(), event.getDescription(),
                event.getCommissionPercent(), event.getCommissionType(), optionNames,
                event.getStatus());
    }

    List<EventInfoDTO> toEventInfoList(Iterable<Event> events) {
        List<EventInfoDTO> eventInfoList = new ArrayList<>();
        for (Event event : events) {
            eventInfoList.add(toEventInfo(event));
        }
        return eventInfoList;
    }

    MarketStateDTO toMarketState(Event event) {
        return new MarketStateDTO(toEventInfo(event), toOptionStates(event),
                event.getAccount().getBalance(), event.getAccount().getTotalCommissionCollected(),
                toTradeHistory(event), event.getWinningOptionName().orElse(null));
    }

    private List<OptionStateDTO> toOptionStates(Event event) {
        List<OptionStateDTO> optionStates = new ArrayList<>();
        for (int index = 0; index < event.getOptionCount(); index++) {
            EventOption option = event.getOptions().get(index);
            optionStates.add(new OptionStateDTO(option.getName(), event.getOptionValue(index),
                    option.getShares()));
        }
        return optionStates;
    }

    /**
     * The trading history of the event, ordered from the latest trade to the first one.
     */
    private List<TradeRecordDTO> toTradeHistory(Event event) {
        List<Trade> trades = event.getTrades();
        List<TradeRecordDTO> history = new ArrayList<>();
        for (int index = trades.size() - 1; index >= 0; index--) {
            Trade trade = trades.get(index);
            history.add(new TradeRecordDTO(event.getOptionName(trade.getOptionIndex()),
                    trade.getShares(), trade.getSharesCost(), trade.getCommission(),
                    trade.getTotalPaid()));
        }
        return history;
    }
}
