package gm.engine.xml;

import gm.dto.CommissionType;
import gm.engine.core.Event;
import gm.engine.core.EventOption;
import gm.engine.core.GuessMarket;
import gm.engine.core.method.LmsrTradingMethod;
import gm.engine.core.method.TradingMethod;
import gm.engine.exception.InvalidCommissionException;
import gm.engine.exception.InvalidLiquidityException;
import gm.engine.exception.InvalidOptionsException;
import gm.engine.exception.MissingXmlDataException;
import gm.engine.xml.generated.XmlCommission;
import gm.engine.xml.generated.XmlEvent;
import gm.engine.xml.generated.XmlEvents;
import gm.engine.xml.generated.XmlGuessMarket;
import gm.engine.xml.generated.XmlLmsr;
import gm.engine.xml.generated.XmlMethod;
import gm.engine.xml.generated.XmlOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the content of a data file into the core objects of the engine, and validates on the way
 * that the content makes sense for the application (the file is guaranteed to match the schema, but
 * not to be logically valid).
 */
public class EventsMapper {

    private static final int MIN_COMMISSION = 0;
    private static final int MAX_COMMISSION = 90;
    private static final int REQUIRED_OPTIONS = 2;

    /**
     * @param xmlSystem the content of the data file
     * @return a new system holding all the events of the file
     */
    public GuessMarket toGuessMarket(XmlGuessMarket xmlSystem) {
        XmlEvents xmlEvents = xmlSystem.getEvents();
        if (xmlEvents == null) {
            throw new MissingXmlDataException("GM-events", "the root element <Guess-Market>");
        }
        List<XmlEvent> xmlEventList = xmlEvents.getEventList();
        if (xmlEventList.isEmpty()) {
            throw new MissingXmlDataException("GM-event",
                    "the element <GM-events> (the file does not describe any event)");
        }

        GuessMarket market = new GuessMarket();
        for (int position = 0; position < xmlEventList.size(); position++) {
            market.addEvent(toEvent(xmlEventList.get(position), position + 1));
        }
        return market;
    }

    private Event toEvent(XmlEvent xmlEvent, int positionInFile) {
        String location = "event number " + positionInFile + " in the file";

        Integer id = xmlEvent.getId();
        if (id == null) {
            throw new MissingXmlDataException("id", location);
        }
        String name = trim(xmlEvent.getName());
        if (name.isEmpty()) {
            throw new MissingXmlDataException("name attribute", "the event with id " + id);
        }
        location = "the event [" + name + "] (id " + id + ")";

        String description = trim(xmlEvent.getDescription());
        if (description.isEmpty()) {
            throw new MissingXmlDataException("description", location);
        }

        int commissionPercent = readCommissionValue(xmlEvent, id, name, location);
        CommissionType commissionType = readCommissionType(xmlEvent, id, name, location);
        List<EventOption> options = readOptions(xmlEvent, id, name, location);
        TradingMethod tradingMethod = readTradingMethod(xmlEvent, id, name, location);

        return new Event(id, name, description, commissionPercent, commissionType, options,
                tradingMethod);
    }

    private int readCommissionValue(XmlEvent xmlEvent, int id, String name, String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        Integer value = commission.getValue();
        if (value == null) {
            throw new MissingXmlDataException("comision", location);
        }
        if (value < MIN_COMMISSION || value > MAX_COMMISSION) {
            throw InvalidCommissionException.outOfRange(id, name, value);
        }
        return value;
    }

    private CommissionType readCommissionType(XmlEvent xmlEvent, int id, String name,
                                              String location) {
        XmlCommission commission = requireCommission(xmlEvent, location);
        String type = trim(commission.getType());
        if (type.isEmpty()) {
            throw new MissingXmlDataException("type attribute of <comision>", location);
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
            throw new MissingXmlDataException("comision", location);
        }
        return commission;
    }

    private List<EventOption> readOptions(XmlEvent xmlEvent, int id, String name, String location) {
        XmlOptions xmlOptions = xmlEvent.getOptions();
        if (xmlOptions == null) {
            throw new MissingXmlDataException("GM-options", location);
        }
        List<String> optionNames = xmlOptions.getOptionList();
        if (optionNames.size() != REQUIRED_OPTIONS) {
            throw new InvalidOptionsException(id, name, optionNames.size());
        }

        List<EventOption> options = new ArrayList<>();
        for (String optionName : optionNames) {
            String trimmedName = trim(optionName);
            if (trimmedName.isEmpty()) {
                throw new MissingXmlDataException("GM-option", location);
            }
            options.add(new EventOption(trimmedName));
        }
        return options;
    }

    private TradingMethod readTradingMethod(XmlEvent xmlEvent, int id, String name,
                                            String location) {
        XmlMethod method = xmlEvent.getMethod();
        if (method == null) {
            throw new MissingXmlDataException("GM-method", location);
        }
        XmlLmsr lmsr = method.getLmsr();
        if (lmsr == null) {
            throw new MissingXmlDataException("GM-LMSR", location);
        }
        Integer liquidity = lmsr.getB();
        if (liquidity == null) {
            throw new MissingXmlDataException("b", location);
        }
        if (liquidity <= 0) {
            throw new InvalidLiquidityException(id, name, liquidity);
        }
        return new LmsrTradingMethod(liquidity);
    }

    /**
     * Cleans a text value that came from the file: spaces at its edges are removed, and a sequence of
     * spaces or line breaks inside it (a text that was written on several lines in the file) becomes
     * a single space.
     */
    private String trim(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
