import gm.dto.CommissionType;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.dto.UserEventDTO;
import gm.engine.impl.GuessMarketEngineImpl;

/**
 * A user creates an event and becomes its market maker, and every rule a data file obeys is checked
 * for a created event too.
 * multiple.xml: events 1-4, users Avrum 1000, Tikva 10000, Menash 100.
 */
public class CreateEventTest extends Check {

    public static void main(String[] args) {
        run("create-event", CreateEventTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.loadEventsFile(DATA + "ex2/multiple.xml");

        // The id comes after the ones the file used, and the creator is the market maker.
        EventInfoDTO created = engine.createEvent(lmsr("Menash", "Snow in Tel Aviv", 5, 50));
        expect(5, created.id(), "the id of the new event");
        expect("Menash", created.marketMakerName(), "the market maker of the new event");
        expect(EventStatus.INACTIVE, created.status(), "a new event starts inactive");
        expect(EventType.LMSR, created.type(), "the type of the new event");
        expect("[Yes, No]", created.optionNames().toString(), "the options of the new event");
        expect(5, engine.getAllEvents().size(), "the system holds the new event");

        // The creator sees it as an event of their own, and can run its whole life cycle.
        expectTrue(isMarketMakerOf(engine, "Menash", 5), "Menash is listed as the market maker");
        engine.openEvent(5, "Menash");
        expect(EventStatus.ACTIVE, statusOf(engine, 5), "the creator can open the event");
        engine.buyShares(5, "Tikva", 0, 10);
        engine.closeEvent(5, "Menash", 0);
        expect(EventStatus.CLOSED, statusOf(engine, 5), "the creator can close the event");

        // An order book event is created out of its own fields, and the next id follows.
        EventInfoDTO orderBook = engine.createEvent(orderBook("Avrum", "Rain tonight", 10, 2, 100));
        expect(6, orderBook.id(), "the id of the second new event");
        expect(EventType.ORDER_BOOK, orderBook.type(), "the type of the second new event");
        engine.openEvent(6, "Avrum");
        expect(EventStatus.ACTIVE, statusOf(engine, 6), "the order book event opens");

        // Every rule a data file obeys is checked here too.
        rejects(engine, lmsr("Menash", "  ", 5, 50), "an event without a name");
        rejects(engine, new NewEventRequestDTO("Menash", "No description", "", 5,
                CommissionType.ON_CLOSE, "Yes", "No", EventType.LMSR, 50, 0, false, 0),
                "an event without a description");
        rejects(engine, new NewEventRequestDTO("Menash", "Same options", "Both alike", 5,
                CommissionType.ON_CLOSE, "Yes", "yes", EventType.LMSR, 50, 0, false, 0),
                "two options with the same name");
        rejects(engine, lmsr("Menash", "Too greedy", 95, 50), "a commission above 90");
        rejects(engine, lmsr("Menash", "No liquidity", 5, 0), "an LMSR event with b = 0");
        rejects(engine, orderBook("Avrum", "No base value", 5, 0, 0), "an order book event with d = 0");
        rejects(engine, orderBook("Avrum", "Not divisible", 5, 3, 100),
                "an initial investment that is not a multiple of d");
        rejects(engine, lmsr("nobody", "Ghost event", 5, 50), "an unknown user");

        // Nothing that was rejected reached the system, so the next id is still the one after 6.
        expect(6, engine.getAllEvents().size(), "the rejected events were not created");
        expect(7, engine.createEvent(lmsr("Tikva", "Still counting", 5, 20)).id(), "the next id");
    }

    static NewEventRequestDTO lmsr(String user, String name, int commission, int liquidity) {
        return new NewEventRequestDTO(user, name, "Created by " + user, commission,
                CommissionType.ON_CLOSE, "Yes", "No", EventType.LMSR, liquidity, 0, false, 0);
    }

    static NewEventRequestDTO orderBook(String user, String name, int commission, int baseValue,
                                        int initial) {
        return new NewEventRequestDTO(user, name, "Created by " + user, commission,
                CommissionType.ON_PURCHASE, "Yes", "No", EventType.ORDER_BOOK, 0, baseValue, true,
                initial);
    }

    static EventStatus statusOf(GuessMarketEngineImpl engine, int eventId) {
        for (EventInfoDTO event : engine.getAllEvents()) {
            if (event.id() == eventId) {
                return event.status();
            }
        }
        throw new AssertionError("no event with id " + eventId);
    }

    static boolean isMarketMakerOf(GuessMarketEngineImpl engine, String userName, int eventId) {
        for (UserEventDTO row : engine.getUserDetails(userName).events()) {
            if (row.event().id() == eventId) {
                return row.marketMaker();
            }
        }
        return false;
    }

    static void rejects(GuessMarketEngineImpl engine, NewEventRequestDTO request, String what) {
        expectThrows(RuntimeException.class, () -> engine.createEvent(request), what);
    }
}
