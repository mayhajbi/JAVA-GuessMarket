import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.engine.exception.InvalidEventDetailsException;
import gm.engine.exception.UserBlockedException;
import gm.engine.impl.GuessMarketEngineImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Whitespace cleanup of a created event, the list of active events, a blocked user creating an
 * event, and a data file with two options of the same name.
 * multiple.xml: events 1-4, users Avrum 1000, Tikva 10000, Menash 100.
 */
public class EventRulesTest extends Check {

    public static void main(String[] args) {
        run("event-rules", EventRulesTest::check);
    }

    static void check() throws IOException {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.loadEventsFile(DATA + "ex2/multiple.xml");

        // A description typed on several lines is cleaned like a value out of a file.
        EventInfoDTO created = engine.createEvent(new NewEventRequestDTO("Avrum", "  Multi \t line ",
                "Line one\nline\ttwo   end", 5, CommissionType.ON_CLOSE, " Yes\n", "No", EventType.LMSR,
                50, 0, false, 0));
        expect("Line one line two end", created.description(), "created event: description");
        expect("Multi line", created.name(), "created event: name");
        expect("[Yes, No]", created.optionNames().toString(), "created event: options");

        // Only the active events are listed.
        engine.openEvent(1, "Tikva");
        expect("[1]", engine.getEvents(new EventFilterDTO(EnumSet.allOf(EventType.class),
                EnumSet.of(EventStatus.ACTIVE), EnumSet.allOf(CommissionType.class))).stream().map(event -> event.id()).toList().toString(),
                "active events");

        // Menash becomes blocked, and then may not create an event.
        engine.buyShares(1, "Menash", 0, 30);
        engine.buyShares(1, "Menash", 1, 400);
        expectThrows(UserBlockedException.class, () -> engine.createEvent(new NewEventRequestDTO("Menash",
                "Blocked", "Blocked user", 5, CommissionType.ON_CLOSE, "Yes", "No", EventType.LMSR, 50, 0,
                false, 0)), "a blocked user cannot create an event");

        // A data file whose two options have the same name (ignoring case) is rejected.
        String xml = Files.readString(Path.of(DATA + "ex2/small.xml"));
        Matcher option = Pattern.compile("<GM-option>(.*?)</GM-option>").matcher(xml);
        option.find();
        String first = option.group(1);
        option.find();
        String duplicated = xml.substring(0, option.start(1)) + first.toUpperCase()
                + xml.substring(option.end(1));
        Path file = Files.createTempFile("dup-options", ".xml");
        try {
            Files.writeString(file, duplicated);
            expectThrows(InvalidEventDetailsException.class,
                    () -> new GuessMarketEngineImpl().loadEventsFile(file.toString()),
                    "same option names in a file");
        } finally {
            Files.delete(file);
        }
    }
}
