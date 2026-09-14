package gm.engine.xml;

import gm.dto.CommissionType;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.EventValidator;
import gm.engine.core.GuessMarket;
import gm.engine.core.User;
import gm.engine.core.method.LmsrTradingMethod;
import gm.engine.core.method.TradingMethod;
import gm.engine.exception.GuessMarketException;
import gm.engine.exception.InvalidCommissionException;
import gm.engine.exception.InvalidInitialCashException;
import gm.engine.exception.InvalidOrderBookException;
import gm.engine.exception.MarketMakerEventNotFoundException;
import gm.engine.exception.MissingMarketMakerException;
import gm.engine.exception.MissingXmlDataException;
import gm.engine.exception.MultipleMarketMakersException;
import gm.engine.util.InputText;
import gm.engine.xml.generated.XmlCommission;
import gm.engine.xml.generated.XmlEvent;
import gm.engine.xml.generated.XmlEvents;
import gm.engine.xml.generated.XmlGuessMarket;
import gm.engine.xml.generated.XmlLmsr;
import gm.engine.xml.generated.XmlMarketMaker;
import gm.engine.xml.generated.XmlMarketMakerEvent;
import gm.engine.xml.generated.XmlMethod;
import gm.engine.xml.generated.XmlOptions;
import gm.engine.xml.generated.XmlOrderBook;
import gm.engine.xml.generated.XmlUser;
import gm.engine.xml.generated.XmlUsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the content of a data file into the core objects of the engine, and validates on the way
 * that the content makes sense for the application (the file is guaranteed to match the schema, but
 * not to be logically valid).
 * <p>
 * The validation runs in a fixed order, top to bottom by the structure of the file, and stops at the
 * first fault: the events, then the users, then the market maker references, and finally that every
 * event has exactly one market maker.
 */
public class EventsMapper {

    private static final String ALLOW_MINT_TRUE = "true";
    private static final String ALLOW_MINT_FALSE = "false";

    /**
     * @param xmlSystem the content of the data file
     * @return a new system holding all the events and users of the file
     */
    public GuessMarket toGuessMarket(XmlGuessMarket xmlSystem) {
        GuessMarket market = new GuessMarket();
        mapEvents(xmlSystem.getEvents(), market);
        mapUsers(xmlSystem.getUsers(), market);
        return market;
    }

    private void mapEvents(XmlEvents xmlEvents, GuessMarket market) {
        if (xmlEvents == null) {
            throw new MissingXmlDataException("GM-events", "the root element <Guess-Market>");
        }
        List<XmlEvent> xmlEventList = xmlEvents.getEventList();
        if (xmlEventList.isEmpty()) {
            throw new MissingXmlDataException("GM-event",
                    "the element <GM-events> (the file does not describe any event)");
        }
        for (int position = 0; position < xmlEventList.size(); position++) {
            market.addEvent(toEvent(xmlEventList.get(position), position + 1));
        }
    }

    private void mapUsers(XmlUsers xmlUsers, GuessMarket market) {
        if (xmlUsers == null) {
            throw new MissingXmlDataException("GM-users", "the root element <Guess-Market>");
        }
        List<XmlUser> xmlUserList = xmlUsers.getUserList();
        if (xmlUserList.isEmpty()) {
            throw new MissingXmlDataException("GM-user",
                    "the element <GM-users> (the file does not describe any user)");
        }
        for (int position = 0; position < xmlUserList.size(); position++) {
            market.addUser(toUser(xmlUserList.get(position), position + 1));
        }

        wireMarketMakers(xmlUserList, market);
        verifyEveryEventHasMarketMaker(market);
    }

    private Event toEvent(XmlEvent xmlEvent, int positionInFile) {
        String location = "event number " + positionInFile + " in the file";

        Integer id = xmlEvent.getId();
        if (id == null) {
            throw new MissingXmlDataException("id", location);
        }
        String name = InputText.normalize(xmlEvent.getName());
        if (name.isEmpty()) {
            throw new MissingXmlDataException("name attribute", "the event with id " + id);
        }
        location = "the event " + GuessMarketException.describeEvent(name, id);

        String description = InputText.normalize(xmlEvent.getDescription());
        if (description.isEmpty()) {
            throw new MissingXmlDataException("description", location);
        }

        int commissionPercent = readCommissionValue(xmlEvent, id, name, location);
        CommissionType commissionType = readCommissionType(xmlEvent, id, name, location);
        List<EventOption> options = readOptions(xmlEvent, id, name, location);

        return buildEvent(xmlEvent, id, name, description, commissionPercent, commissionType,
                options, location);
    }

    private int readCommissionValue(XmlEvent xmlEvent, int id, String name, String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        Integer value = commission.getValue();
        if (value == null) {
            throw new MissingXmlDataException("commission", location);
        }
        EventValidator.requireCommissionInRange(id, name, value);
        return value;
    }

    private CommissionType readCommissionType(XmlEvent xmlEvent, int id, String name,
                                              String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        String type = InputText.normalize(commission.getType());
        if (type.isEmpty()) {
            throw new MissingXmlDataException("type attribute of <commission>", location);
        }
        for (CommissionType commissionType : CommissionType.values()) {
            if (commissionType.getDisplayName().equalsIgnoreCase(type)) {
                return commissionType;
            }
        }
        throw InvalidCommissionException.unknownType(id, name, type);
    }

    private XmlCommission requireCommission(XmlEvent xmlEvent, String location) {
        XmlCommission commission = xmlEvent.getCommission();
        if (commission == null) {
            throw new MissingXmlDataException("commission", location);
        }
        return commission;
    }

    private List<EventOption> readOptions(XmlEvent xmlEvent, int id, String name, String location) {
        XmlOptions xmlOptions = xmlEvent.getOptions();
        if (xmlOptions == null) {
            throw new MissingXmlDataException("GM-options", location);
        }
        List<String> optionNames = xmlOptions.getOptionList();
        EventValidator.requireTwoOptions(id, name, optionNames.size());

        List<EventOption> options = new ArrayList<>();
        for (String optionName : optionNames) {
            String trimmedName = InputText.normalize(optionName);
            if (trimmedName.isEmpty()) {
                throw new MissingXmlDataException("GM-option", location);
            }
            options.add(new EventOption(trimmedName));
        }
        EventValidator.requireOptionNames(id, name, options.get(0).getName(), options.get(1).getName());
        return options;
    }

    private Event buildEvent(XmlEvent xmlEvent, int id, String name, String description,
                             int commissionPercent, CommissionType commissionType,
                             List<EventOption> options, String location) {
        XmlMethod method = xmlEvent.getMethod();
        if (method == null) {
            throw new MissingXmlDataException("GM-method", location);
        }

        XmlLmsr lmsr = method.getLmsr();
        XmlOrderBook orderBook = method.getOrderBook();
        if (lmsr != null) {
            TradingMethod tradingMethod = readLmsr(lmsr, id, name, location);
            return Event.lmsr(id, name, description, commissionPercent, commissionType, options,
                    tradingMethod);
        }
        if (orderBook != null) {
            int baseValue = readOrderBookBaseValue(orderBook, id, name, location);
            int initialInvestment = readOrderBookInitialInvestment(orderBook, location);
            EventValidator.requireInitialInvestment(id, name, initialInvestment, baseValue);
            boolean allowMint = readOrderBookAllowMint(orderBook, id, name, location);
            return Event.orderBook(id, name, description, commissionPercent, commissionType, options,
                    baseValue, allowMint, initialInvestment);
        }
        throw new MissingXmlDataException("GM-LMSR or GM-order-book", location);
    }

    private TradingMethod readLmsr(XmlLmsr lmsr, int id, String name, String location) {
        Integer liquidity = lmsr.getB();
        if (liquidity == null) {
            throw new MissingXmlDataException("b", location);
        }
        EventValidator.requireLiquidityPositive(id, name, liquidity);
        return new LmsrTradingMethod(liquidity);
    }

    private int readOrderBookBaseValue(XmlOrderBook orderBook, int id, String name, String location) {
        Integer d = orderBook.getD();
        if (d == null) {
            throw new MissingXmlDataException("d attribute of <GM-order-book>", location);
        }
        EventValidator.requireBaseValuePositive(id, name, d);
        return d;
    }

    private int readOrderBookInitialInvestment(XmlOrderBook orderBook, String location) {
        Integer initial = orderBook.getInitial();
        if (initial == null) {
            throw new MissingXmlDataException("initial attribute of <GM-order-book>", location);
        }
        return initial;
    }

    private boolean readOrderBookAllowMint(XmlOrderBook orderBook, int id, String name,
                                           String location) {
        String value = InputText.normalize(orderBook.getAllowMint());
        if (value.isEmpty()) {
            throw new MissingXmlDataException("allow-mint attribute of <GM-order-book>", location);
        }
        if (value.equalsIgnoreCase(ALLOW_MINT_TRUE)) {
            return true;
        }
        if (value.equalsIgnoreCase(ALLOW_MINT_FALSE)) {
            return false;
        }
        throw InvalidOrderBookException.allowMintNotBoolean(id, name, value);
    }

    private User toUser(XmlUser xmlUser, int positionInFile) {
        String location = "user number " + positionInFile + " in the file";

        String name = InputText.normalize(xmlUser.getName());
        if (name.isEmpty()) {
            throw new MissingXmlDataException("name attribute", location);
        }
        location = "the user [" + name + "]";

        Integer initialCash = xmlUser.getInitialCash();
        if (initialCash == null) {
            throw new MissingXmlDataException("initial-cash", location);
        }
        if (initialCash <= 0) {
            throw new InvalidInitialCashException(name, initialCash);
        }
        return new User(name, initialCash);
    }

    private void wireMarketMakers(List<XmlUser> xmlUserList, GuessMarket market) {
        for (XmlUser xmlUser : xmlUserList) {
            XmlMarketMaker marketMaker = xmlUser.getMarketMaker();
            if (marketMaker == null) {
                continue;
            }
            User user = market.getUser(InputText.normalize(xmlUser.getName()));
            for (XmlMarketMakerEvent reference : marketMaker.getEventList()) {
                Integer eventId = reference.getId();
                if (eventId == null) {
                    throw new MissingXmlDataException("id attribute of <event>",
                            "the market-maker block of the user [" + user.getName() + "]");
                }
                Event event = market.findEvent(eventId);
                if (event == null) {
                    throw new MarketMakerEventNotFoundException(user.getName(), eventId);
                }
                if (event.getMarketMaker() != null) {
                    throw new MultipleMarketMakersException(event.getId(), event.getName(),
                            event.getMarketMaker().getName(), user.getName());
                }
                event.setMarketMaker(user);
            }
        }
    }

    private void verifyEveryEventHasMarketMaker(GuessMarket market) {
        for (Event event : market.getAllEvents()) {
            if (event.getMarketMaker() == null) {
                throw new MissingMarketMakerException(event.getId(), event.getName());
            }
        }
    }
}
