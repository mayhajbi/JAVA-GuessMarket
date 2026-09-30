import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventInfoDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import gm.engine.impl.GuessMarketEngineImpl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Filtering events in the engine, the base of the events list of the server.
 * multiple.xml: 1 LMSR on-purchase, 2 OB on-close, 3 OB on-purchase, 4 LMSR on-close.
 */
public class EventFilterTest extends Check {

    static final Set<EventType> ALL_TYPES = EnumSet.allOf(EventType.class);
    static final Set<EventStatus> ALL_STATUSES = EnumSet.allOf(EventStatus.class);
    static final Set<CommissionType> ALL_COMMISSIONS = EnumSet.allOf(CommissionType.class);

    public static void main(String[] args) {
        run("event-filter", EventFilterTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.loadEventsFile(DATA + "ex2/multiple.xml");

        expect("[1, 2, 3, 4]", ids(engine, ALL_TYPES, ALL_STATUSES, ALL_COMMISSIONS), "everything selected");
        expect("[1, 4]", ids(engine, EnumSet.of(EventType.LMSR), ALL_STATUSES, ALL_COMMISSIONS), "LMSR only");
        expect("[2, 4]", ids(engine, ALL_TYPES, ALL_STATUSES, EnumSet.of(CommissionType.ON_CLOSE)), "on-close only");
        expect("[]", ids(engine, ALL_TYPES, EnumSet.of(EventStatus.ACTIVE), ALL_COMMISSIONS), "nothing active yet");
        engine.openEvent(1, "Tikva");
        expect("[1]", ids(engine, ALL_TYPES, EnumSet.of(EventStatus.ACTIVE), ALL_COMMISSIONS), "active after open");
        expect("[3]", ids(engine, ALL_TYPES, EnumSet.of(EventStatus.INACTIVE),
                EnumSet.of(CommissionType.ON_PURCHASE)), "inactive and on-purchase");
        expect("[]", ids(engine, EnumSet.noneOf(EventType.class), ALL_STATUSES, ALL_COMMISSIONS), "empty selection");
    }

    static String ids(GuessMarketEngineImpl engine, Set<EventType> types, Set<EventStatus> statuses,
                      Set<CommissionType> commissions) {
        List<Integer> ids = new ArrayList<>();
        for (EventInfoDTO event : engine.getEvents(new EventFilterDTO(types, statuses, commissions))) {
            ids.add(event.id());
        }
        return ids.toString();
    }
}
