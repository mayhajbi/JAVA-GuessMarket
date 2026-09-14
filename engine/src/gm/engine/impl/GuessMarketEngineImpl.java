package gm.engine.impl;

import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.HistoryPointDTO;
import gm.dto.LoadResultDTO;
import gm.dto.MarketStateDTO;
import gm.dto.NewEventRequestDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.PriceHistoryDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserDetailsDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.EventValidator;
import gm.engine.core.GuessMarket;
import gm.engine.core.Trade;
import gm.engine.core.User;
import gm.engine.core.method.LmsrTradingMethod;
import gm.engine.core.orderbook.OrderOutcome;
import gm.engine.exception.InvalidEventDetailsException;
import gm.engine.exception.InvalidOrderException;
import gm.engine.exception.NoSystemLoadedException;
import gm.engine.state.SystemStateSerializer;
import gm.engine.util.InputText;
import gm.engine.xml.EventsFileLoader;

import java.util.EnumSet;
import java.util.List;

/**
 * The engine of the system: it holds the loaded system, runs the requested logic on it and answers
 * with data transfer objects only.
 */
public class GuessMarketEngineImpl implements GuessMarketEngine {

    private final EventsFileLoader fileLoader = new EventsFileLoader();
    private final SystemStateSerializer stateSerializer = new SystemStateSerializer();
    private final DtoFactory dtoFactory = new DtoFactory();

    private GuessMarket market;

    @Override
    public LoadResultDTO loadEventsFile(String xmlFilePath) {
        GuessMarket loadedMarket = fileLoader.loadFile(xmlFilePath);
        this.market = loadedMarket;
        return new LoadResultDTO(InputText.cleanPath(xmlFilePath), loadedMarket.getEventCount(),
                loadedMarket.getAllUsers().size(), loadedMarket.getTotalSubsidy());
    }

    @Override
    public List<EventInfoDTO> getAllEvents() {
        return dtoFactory.toEventInfoList(requireLoadedMarket().getAllEvents());
    }

    @Override
    public List<EventInfoDTO> getActiveEvents() {
        return getEvents(new EventFilterDTO(EnumSet.allOf(EventType.class), EnumSet.of(EventStatus.ACTIVE),
                EnumSet.allOf(CommissionType.class)));
    }

    @Override
    public List<EventInfoDTO> getEvents(EventFilterDTO filter) {
        GuessMarket loadedMarket = requireLoadedMarket();
        if (filter == null) {
            return dtoFactory.toEventInfoList(loadedMarket.getAllEvents());
        }
        return dtoFactory.toEventInfoList(
                loadedMarket.getEvents(filter.types(), filter.statuses(), filter.commissionTypes()));
    }

    @Override
    public List<UserInfoDTO> getAllUsers() {
        return dtoFactory.toUserInfoList(requireLoadedMarket().getAllUsers());
    }

    @Override
    public UserDetailsDTO getUserDetails(String userName) {
        return dtoFactory.toUserDetails(requireUser(userName), requireLoadedMarket().getAllEvents());
    }

    @Override
    public MarketStateDTO getMarketState(int eventId) {
        return dtoFactory.toMarketState(requireEvent(eventId));
    }

    @Override
    public OrderBookStateDTO getOrderBookState(int eventId) {
        return dtoFactory.toOrderBookState(requireEvent(eventId));
    }

    @Override
    public List<PriceHistoryDTO> getEventPriceHistory(int eventId) {
        return dtoFactory.toPriceHistory(requireEvent(eventId));
    }

    @Override
    public List<HistoryPointDTO> getUserBalanceHistory(String userName) {
        return dtoFactory.toBalanceHistory(requireUser(userName));
    }

    @Override
    public EventInfoDTO createEvent(NewEventRequestDTO request) {
        if (request == null) {
            throw InvalidEventDetailsException.missingName();
        }
        User marketMaker = requireUser(request.userName());
        marketMaker.requireNotBlocked("create events");

        GuessMarket loadedMarket = requireLoadedMarket();
        int id = loadedMarket.nextEventId();
        String name = InputText.normalize(request.name());
        EventValidator.requireName(name);
        EventValidator.requireDescription(name, request.description());
        EventValidator.requireOptionNames(id, name, request.firstOption(), request.secondOption());
        EventValidator.requireCommissionInRange(id, name, request.commissionPercent());
        Event event = buildEvent(request, id, name);
        event.setMarketMaker(marketMaker);
        loadedMarket.addEvent(event);
        return dtoFactory.toEventInfo(event);
    }

    /**
     * Builds the event itself, once its shared details are known to be legal, out of the fields that
     * belong to the trading method the user chose. The texts are cleaned the same way a value out of
     * a data file is cleaned.
     */
    private Event buildEvent(NewEventRequestDTO request, int id, String name) {
        String description = InputText.normalize(request.description());
        List<EventOption> options = List.of(new EventOption(InputText.normalize(request.firstOption())),
                new EventOption(InputText.normalize(request.secondOption())));

        if (request.type() == EventType.LMSR) {
            EventValidator.requireLiquidityPositive(id, name, request.liquidity());
            return Event.lmsr(id, name, description, request.commissionPercent(),
                    request.commissionType(), options, new LmsrTradingMethod(request.liquidity()));
        }
        EventValidator.requireBaseValuePositive(id, name, request.baseValue());
        EventValidator.requireInitialInvestment(id, name, request.initialInvestment(),
                request.baseValue());
        return Event.orderBook(id, name, description, request.commissionPercent(),
                request.commissionType(), options, request.baseValue(), request.allowMint(),
                request.initialInvestment());
    }

    @Override
    public EventInfoDTO openEvent(int eventId, String userName) {
        Event event = requireEvent(eventId);
        event.open(requireUser(userName));
        return dtoFactory.toEventInfo(event);
    }

    @Override
    public PurchaseResultDTO buyShares(int eventId, String userName, int optionIndex, long quantity) {
        Event event = requireEvent(eventId);
        User buyer = requireUser(userName);
        Trade trade = event.buy(buyer, optionIndex, quantity);
        return new PurchaseResultDTO(event.getOptionName(trade.getOptionIndex()), trade.getShares(),
                trade.getSharesCost(), trade.getCommission(), trade.getTotalPaid(),
                buyer.getAccount().getBalance(), buyer.getAccount().isBlocked(),
                dtoFactory.toMarketState(event));
    }

    @Override
    public OrderResultDTO submitOrder(OrderRequestDTO request) {
        if (request == null) {
            throw InvalidOrderException.missingRequest();
        }
        Event event = requireEvent(request.eventId());
        User user = requireUser(request.userName());
        OrderOutcome outcome = event.placeOrder(user, request.side(), request.optionIndex(),
                request.quantity(), request.price());
        return new OrderResultDTO(dtoFactory.toOrderBookTrades(event, outcome.trades()),
                outcome.filledQuantity(), outcome.restingQuantity(), user.getAccount().getBalance(),
                user.getAccount().isBlocked());
    }

    @Override
    public EventInfoDTO closeEvent(int eventId, String userName, int winningOptionIndex) {
        Event event = requireEvent(eventId);
        event.close(requireUser(userName), winningOptionIndex);
        return dtoFactory.toEventInfo(event);
    }

    @Override
    public String saveSystemState(String pathWithoutExtension) {
        return stateSerializer.save(requireLoadedMarket(), pathWithoutExtension);
    }

    @Override
    public String loadSystemState(String pathWithoutExtension) {
        GuessMarket loadedMarket = stateSerializer.load(pathWithoutExtension);
        this.market = loadedMarket;
        return stateSerializer.resolveStateFilePath(pathWithoutExtension);
    }

    private GuessMarket requireLoadedMarket() {
        if (market == null) {
            throw new NoSystemLoadedException();
        }
        return market;
    }

    private Event requireEvent(int eventId) {
        return requireLoadedMarket().getEvent(eventId);
    }

    private User requireUser(String userName) {
        return requireLoadedMarket().getUser(userName);
    }
}
