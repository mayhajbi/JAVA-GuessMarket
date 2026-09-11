package gm.engine.impl;

import gm.dto.EventInfoDTO;
import gm.dto.LoadResultDTO;
import gm.dto.MarketStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.engine.core.Event;
import gm.engine.core.GuessMarket;
import gm.engine.core.Trade;
import gm.engine.core.User;
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
        return new LoadResultDTO(cleanPath, loadedMarket.getEventCount(), loadedMarket.getTotalSubsidy());
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
    public List<UserInfoDTO> getAllUsers() {
        return dtoFactory.toUserInfoList(requireLoadedMarket().getAllUsers());
    }

    @Override
    public MarketStateDTO getMarketState(int eventId) {
        return dtoFactory.toMarketState(requireLoadedMarket().getEvent(eventId));
    }

    @Override
    public MarketStateDTO openEvent(int eventId, String userName) {
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(eventId);
        event.open(loadedMarket.getUser(userName));
        return dtoFactory.toMarketState(event);
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
    public MarketStateDTO closeEvent(int eventId, String userName, int winningOptionIndex) {
        GuessMarket loadedMarket = requireLoadedMarket();
        Event event = loadedMarket.getEvent(eventId);
        event.close(loadedMarket.getUser(userName), winningOptionIndex);
        return dtoFactory.toMarketState(event);
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
