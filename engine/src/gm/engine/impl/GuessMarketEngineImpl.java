package gm.engine.impl;

import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
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
import gm.dto.EventType;
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
        String cleanPath = xmlFilePath == null ? "" : InputText.stripSurroundingQuotes(xmlFilePath.trim());
        return new LoadResultDTO(cleanPath, loadedMarket.getEventCount(),
                loadedMarket.getAllUsers().size(), loadedMarket.getTotalSubsidy());
    }

    @Override
    public List<EventInfoDTO> getAllEvents() {
        return dtoFactory.toEventInfoList(requireLoadedMarket().getAllEvents());
    }

    @Override
    public List<EventInfoDTO> getActiveEvents() {
        return dtoFactory.toEventInfoList(requireLoadedMarket().getActiveEvents());
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
        GuessMarket loadedMarket = requireLoadedMarket();
        return dtoFactory.toUserDetails(loadedMarket.getUser(userName), loadedMarket.getAllEvents());
    }

    @Override
    public MarketStateDTO getMarketState(int eventId) {
        return dtoFactory.toMarketState(requireLoadedMarket().getEvent(eventId));
    }

    @Override
    public OrderBookStateDTO getOrderBookState(int eventId) {
        return dtoFactory.toOrderBookState(requireLoadedMarket().getEvent(eventId));
    }

    @Override
    public List<PriceHistoryDTO> getEventPriceHistory(int eventId) {
        return dtoFactory.toPriceHistory(requireLoadedMarket().getEvent(eventId));
    }

    @Override
    public List<HistoryPointDTO> getUserBalanceHistory(String userName) {
        return dtoFactory.toBalanceHistory(requireLoadedMarket().getUser(userName));
    }

    @Override
    public EventInfoDTO createEvent(NewEventRequestDTO request) {
        if (request == null) {
            throw InvalidEventDetailsException.missingName();
        }
        GuessMarket loadedMarket = requireLoadedMarket();
        User marketMaker = loadedMarket.getUser(request.userName());

        String name = trim(request.name());
        EventValidator.requireName(name);
        EventValidator.requireDescription(name, request.description());
        EventValidator.requireOptionNames(name, request.firstOption(), request.secondOption());

        int id = loadedMarket.nextEventId();
        EventValidator.requireCommissionInRange(id, name, request.commissionPercent());
        Event event = buildEvent(request, id, name);
        event.setMarketMaker(marketMaker);
        loadedMarket.addEvent(event);
        return dtoFactory.toEventInfo(event);
    }

    /**
     * Builds the event itself, once its shared details are known to be legal, out of the fields that
     * belong to the trading method the user chose.
     */
    private Event buildEvent(NewEventRequestDTO request, int id, String name) {
        String description = trim(request.description());
        List<EventOption> options = List.of(new EventOption(trim(request.firstOption())),
                new EventOption(trim(request.secondOption())));

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

    /**
     * Cleans a value a user typed the same way a value out of a data file is cleaned.
     */
    private static String trim(String value) {
        return value == null ? "" : value.trim().replaceAll("\s+", " ");
    }

    @Override
    public EventInfoDTO openEvent(int eventId, String userName) {
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(eventId);
        event.open(loadedMarket.getUser(userName));
        return dtoFactory.toEventInfo(event);
    }

    @Override
    public PurchaseResultDTO buyShares(int eventId, String userName, int optionIndex, long quantity) {
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(eventId);
        User buyer = loadedMarket.getUser(userName);
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
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(request.eventId());
        User user = loadedMarket.getUser(request.userName());
        OrderOutcome outcome = event.placeOrder(user, request.side(), request.optionIndex(),
                request.quantity(), request.price());
        return new OrderResultDTO(dtoFactory.toOrderBookTrades(event, outcome.trades()),
                outcome.filledQuantity(), outcome.restingQuantity(), user.getAccount().getBalance(),
                user.getAccount().isBlocked(), dtoFactory.toOrderBookState(event));
    }

    @Override
    public EventInfoDTO closeEvent(int eventId, String userName, int winningOptionIndex) {
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(eventId);
        event.close(loadedMarket.getUser(userName), winningOptionIndex);
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
}
