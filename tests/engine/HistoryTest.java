import gm.dto.HistoryPointDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderSide;
import gm.dto.PriceHistoryDTO;
import gm.dto.UserInfoDTO;
import gm.engine.impl.GuessMarketEngineImpl;

import java.util.List;

/**
 * The price history of an event and the balance history of a user, the base of the price chart and
 * of the balance chart.
 * The engine starts as {@link Scenario#multiple()}: 1 LMSR (b=100, Tikva), 2 OB (d=1, initial=100, mint,
 * Avrum), users Tikva 10000, Avrum 1000, Menash 100.
 */
public class HistoryTest extends Check {

    public static void main(String[] args) {
        run("history", HistoryTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = Scenario.multiple();

        // A user that only loaded funds has two points: the empty account, and the amount that was loaded.
        expect(2, engine.getUserBalanceHistory("Menash").size(), "Menash points at the start");
        expect(0.0, engine.getUserBalanceHistory("Menash").get(0).value(), "Menash starts with an empty account");
        expect(100.0, last(engine.getUserBalanceHistory("Menash")), "Menash balance at the start");

        // An LMSR event has no price at all before its market maker opens it.
        expect(0, points(engine, 1, 0).size(), "event 1 option 1 before open");
        expect(0, points(engine, 1, 1).size(), "event 1 option 2 before open");

        engine.openEvent(1, "Tikva");
        expect(1, points(engine, 1, 0).size(), "event 1 option 1 after open");
        expect(0.5, last(points(engine, 1, 0)), "option 1 value after open");
        expect(0.5, last(points(engine, 1, 1)), "option 2 value after open");

        // Buying pushes the bought option up and the other one down, and adds one point to both.
        engine.buyShares(1, "Menash", 0, 10);
        expect(2, points(engine, 1, 0).size(), "event 1 option 1 after the purchase");
        expect(2, points(engine, 1, 1).size(), "event 1 option 2 after the purchase");
        expectTrue(last(points(engine, 1, 0)) > 0.5, "option 1 went up");
        expectTrue(last(points(engine, 1, 1)) < 0.5, "option 2 went down");
        expect(1.0, round(last(points(engine, 1, 0)) + last(points(engine, 1, 1))), "the two values sum to 1");
        expect(3, engine.getUserBalanceHistory("Menash").size(), "Menash points after the purchase");

        // A closed event is worth what it actually pays: the full payout, or nothing.
        engine.closeEvent(1, "Tikva", 0);
        expect(3, points(engine, 1, 0).size(), "event 1 option 1 after close");
        expect(1.0, last(points(engine, 1, 0)), "the winning option is worth the payout");
        expect(0.0, last(points(engine, 1, 1)), "the losing option is worth nothing");

        // An order book option has no price until both a bid and an ask are waiting for it.
        engine.openEvent(2, "Avrum");
        expect(0, points(engine, 2, 0).size(), "event 2 option 1 after open");
        engine.submitOrder(new OrderRequestDTO(2, "Menash", OrderSide.BUY, 0, 5, 0.40));
        expect(0, points(engine, 2, 0).size(), "event 2 option 1 with only a bid");
        engine.submitOrder(new OrderRequestDTO(2, "Avrum", OrderSide.SELL, 0, 5, 0.60));
        expect(1, points(engine, 2, 0).size(), "event 2 option 1 with a bid and an ask");
        expect(0.5, last(points(engine, 2, 0)), "the mid price of option 1");
        expect(0, points(engine, 2, 1).size(), "event 2 option 2 was never quoted");

        // Whatever happened, the last point of a user is the balance the engine reports right now.
        for (UserInfoDTO user : engine.getAllUsers()) {
            expect(round(user.balance()), round(last(engine.getUserBalanceHistory(user.name()))),
                    "the last point of " + user.name());
        }

        // A graph holds its latest 50 points only, and the last of them is still the state right now.
        for (int deposit = 0; deposit < 60; deposit++) {
            engine.deposit("Menash", 1);
        }
        expect(50, engine.getUserBalanceHistory("Menash").size(), "the balance history keeps the latest 50 points");
        expect(round(engine.deposit("Menash", 1).balance()), round(last(engine.getUserBalanceHistory("Menash"))),
                "the last balance point after many deposits");
        engine.openEvent(4, "Tikva");
        for (int purchase = 0; purchase < 60; purchase++) {
            engine.buyShares(4, "Tikva", 0, 1);
        }
        expect(50, points(engine, 4, 0).size(), "the price history keeps the latest 50 points");
    }

    static List<HistoryPointDTO> points(GuessMarketEngineImpl engine, int eventId, int optionIndex) {
        List<PriceHistoryDTO> series = engine.getEventPriceHistory(eventId);
        return series.get(optionIndex).points();
    }

    static double last(List<HistoryPointDTO> points) {
        return points.get(points.size() - 1).value();
    }

    static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
