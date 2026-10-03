package gm.engine.xml;

import gm.dto.CommissionType;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.EventValidator;
import gm.engine.core.method.LmsrTradingMethod;
import gm.engine.core.method.TradingMethod;
import gm.engine.exception.GuessMarketException;
import gm.engine.exception.FileLoadException;
import gm.engine.util.InputText;
import gm.engine.xml.generated.XmlCommission;
import gm.engine.xml.generated.XmlEvent;
import gm.engine.xml.generated.XmlEvents;
import gm.engine.xml.generated.XmlGuessMarket;
import gm.engine.xml.generated.XmlLmsr;
import gm.engine.xml.generated.XmlMethod;
import gm.engine.xml.generated.XmlOptions;
import gm.engine.xml.generated.XmlOrderBook;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the content of a data file into the core objects of the engine, and validates on the way
 * that the content makes sense for the application (the file is not guaranteed to be logically valid).
 * <p>
 * The validation runs in a fixed order, top to bottom by the structure of the file, and stops at the
 * first fault.
 */
public class EventsMapper {

    /** An event of a file has no id yet: the system gives it one when the event is added. */
    private static final int NO_ID = 0;
    private static final String ALLOW_MINT_TRUE = "true";
    private static final String ALLOW_MINT_FALSE = "false";
    private static final String ORDER_BOOK_ELEMENT = "GM-order-book";
    /** How a message points at an event that has no name to call it by. */
    private static final String UNNAMED_EVENT = "one of the events of the file";

    /**
     * Converts the events of a file. A file describes events only: it has no users and no event ids,
     * so the events have no id yet (0). Nothing is added to any system here.
     *
     * @return the events, in the order of the file, without a market maker and without an id
     * @throws FileLoadException when the file has users or event ids
     */
    public List<Event> toEvents(XmlGuessMarket xmlSystem) {
        if (xmlSystem.getUsers() != null) {
            throw FileLoadException.unsupportedFormat("the file has the element 'GM-users'",
                    "Users register by logging in, so a file may describe events only.");
        }
        List<Event> events = new ArrayList<>();
        for (XmlEvent xmlEvent : requireEventList(xmlSystem.getEvents())) {
            if (xmlEvent.getId() != null) {
                String name = InputText.normalize(xmlEvent.getName());
                throw FileLoadException.unsupportedFormat((name.isEmpty() ? UNNAMED_EVENT
                        : "the event " + GuessMarketException.describeEvent(name)) + " has the element 'id'",
                        "Events are identified by their names.");
            }
            events.add(toEvent(xmlEvent));
        }
        return events;
    }

    private List<XmlEvent> requireEventList(XmlEvents xmlEvents) {
        if (xmlEvents == null) {
            throw FileLoadException.missingElement("GM-events", "the root element 'Guess-Market'");
        }
        List<XmlEvent> xmlEventList = xmlEvents.getEventList();
        if (xmlEventList.isEmpty()) {
            throw FileLoadException.missingElement("GM-event", "the element 'GM-events'");
        }
        return xmlEventList;
    }

    private Event toEvent(XmlEvent xmlEvent) {
        String name = InputText.normalize(xmlEvent.getName());
        if (name.isEmpty()) {
            throw FileLoadException.missingAttribute("name", "GM-event", UNNAMED_EVENT);
        }
        EventValidator.requireEnglish(name);
        String location = "the event " + GuessMarketException.describeEvent(name);

        String description = InputText.normalize(xmlEvent.getDescription());
        if (description.isEmpty()) {
            throw FileLoadException.missingElement("description", location);
        }
        EventValidator.requireEnglish(description);

        int commissionPercent = readCommissionValue(xmlEvent, name, location);
        CommissionType commissionType = readCommissionType(xmlEvent, name, location);
        List<EventOption> options = readOptions(xmlEvent, name, location);

        return buildEvent(xmlEvent, name, description, commissionPercent, commissionType, options, location);
    }

    private int readCommissionValue(XmlEvent xmlEvent, String name, String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        Integer value = commission.getValue();
        if (value == null) {
            throw FileLoadException.missingElement("commission", location);
        }
        EventValidator.requireCommissionInRange(value);
        return value;
    }

    private CommissionType readCommissionType(XmlEvent xmlEvent, String name,
                                              String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        String type = InputText.normalize(commission.getType());
        if (type.isEmpty()) {
            throw FileLoadException.missingAttribute("type", "commission", location);
        }
        for (CommissionType commissionType : CommissionType.values()) {
            if (commissionType.getDisplayName().equalsIgnoreCase(type)) {
                return commissionType;
            }
        }
        throw FileLoadException.unknownCommissionType(name, type);
    }

    private XmlCommission requireCommission(XmlEvent xmlEvent, String location) {
        XmlCommission commission = xmlEvent.getCommission();
        if (commission == null) {
            throw FileLoadException.missingElement("commission", location);
        }
        return commission;
    }

    private List<EventOption> readOptions(XmlEvent xmlEvent, String name, String location) {
        XmlOptions xmlOptions = xmlEvent.getOptions();
        if (xmlOptions == null) {
            throw FileLoadException.missingElement("GM-options", location);
        }
        List<String> optionNames = xmlOptions.getOptionList();
        EventValidator.requireTwoOptions(optionNames.size());

        List<EventOption> options = new ArrayList<>();
        for (String optionName : optionNames) {
            String trimmedName = InputText.normalize(optionName);
            if (trimmedName.isEmpty()) {
                throw FileLoadException.missingElement("GM-option", location);
            }
            options.add(new EventOption(trimmedName));
        }
        EventValidator.requireOptionNames(name, options.get(0).getName(), options.get(1).getName());
        return options;
    }

    private Event buildEvent(XmlEvent xmlEvent, String name, String description,
                             int commissionPercent, CommissionType commissionType,
                             List<EventOption> options, String location) {
        XmlMethod method = xmlEvent.getMethod();
        if (method == null) {
            throw FileLoadException.missingElement("GM-method", location);
        }

        XmlLmsr lmsr = method.getLmsr();
        XmlOrderBook orderBook = method.getOrderBook();
        if (lmsr != null) {
            TradingMethod tradingMethod = readLmsr(lmsr, name, location);
            return Event.lmsr(NO_ID, name, description, commissionPercent, commissionType, options,
                    tradingMethod);
        }
        if (orderBook != null) {
            int baseValue = readOrderBookBaseValue(orderBook, name, location);
            int initialInvestment = readOrderBookInitialInvestment(orderBook, location);
            EventValidator.requireInitialInvestment(name, initialInvestment, baseValue);
            boolean allowMint = readOrderBookAllowMint(orderBook, name, location);
            return Event.orderBook(NO_ID, name, description, commissionPercent, commissionType, options,
                    baseValue, allowMint, initialInvestment);
        }
        throw FileLoadException.missingEitherElement("GM-LMSR", ORDER_BOOK_ELEMENT, location);
    }

    private TradingMethod readLmsr(XmlLmsr lmsr, String name, String location) {
        Integer liquidity = lmsr.getB();
        if (liquidity == null) {
            throw FileLoadException.missingElement("b", location);
        }
        EventValidator.requireLiquidityPositive(liquidity);
        return new LmsrTradingMethod(liquidity);
    }

    private int readOrderBookBaseValue(XmlOrderBook orderBook, String name, String location) {
        Integer d = orderBook.getD();
        if (d == null) {
            throw FileLoadException.missingAttribute("d", ORDER_BOOK_ELEMENT, location);
        }
        EventValidator.requireBaseValuePositive(d);
        return d;
    }

    private int readOrderBookInitialInvestment(XmlOrderBook orderBook, String location) {
        Integer initial = orderBook.getInitial();
        if (initial == null) {
            throw FileLoadException.missingAttribute("initial", ORDER_BOOK_ELEMENT, location);
        }
        return initial;
    }

    private boolean readOrderBookAllowMint(XmlOrderBook orderBook, String name,
                                           String location) {
        String value = InputText.normalize(orderBook.getAllowMint());
        if (value.isEmpty()) {
            throw FileLoadException.missingAttribute("allow-mint", ORDER_BOOK_ELEMENT, location);
        }
        if (value.equalsIgnoreCase(ALLOW_MINT_TRUE)) {
            return true;
        }
        if (value.equalsIgnoreCase(ALLOW_MINT_FALSE)) {
            return false;
        }
        throw FileLoadException.allowMintNotBoolean(name, value);
    }
}
