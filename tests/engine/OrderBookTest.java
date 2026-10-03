import gm.dto.AccountEntryDTO;
import gm.dto.AccountEntryType;
import gm.dto.OrderBookOptionDTO;
import gm.dto.OrderBookParticipantDTO;
import gm.dto.OrderBookStateDTO;
import gm.dto.OrderDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderResultDTO;
import gm.dto.OrderSide;
import gm.dto.UserInfoDTO;
import gm.engine.api.GuessMarketEngine;
import gm.engine.exception.EventException;
import gm.engine.exception.UserAccountException;
import gm.engine.exception.UserInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static gm.dto.OrderSide.BUY;
import static gm.dto.OrderSide.SELL;

/**
 * The order book trading method: replays clob_simulation.html (data/clob_simulation.html) step by step
 * in both commission modes and compares books, statistics, holdings and final balances with its ledger,
 * then the edge cases: rejected orders, blocked users, minting, and the order book parameters.
 */
public class OrderBookTest extends Check {

    static final int YES = 0;
    static final int NO = 1;

    public static void main(String[] args) {
        run("order-book", () -> {
            simulation(true);
            simulation(false);
            edgeCases();
            accountRows();
        });
    }

    static void simulation(boolean onPurchase) {
        String mode = onPurchase ? "[on-purchase] " : "[on-close] ";
        GuessMarketEngine engine = onPurchase ? Scenario.clobOnPurchase() : Scenario.clobOnClose();

        // step 1: Zoe mints the first 100 pairs
        engine.openEvent(1, "Zoe");
        near(400, balance(engine, "Zoe"), mode + "Zoe paid 100 for the initial pairs");
        OrderBookStateDTO s = engine.getOrderBookState(1);
        near(100, s.eventInfo().accountBalance(), mode + "event account 100");
        shares(s, "Zoe", 100, 100, mode + "Zoe holds 100 YES + 100 NO");

        // steps 2-5: bids and asks rest
        order(engine, 1, "Bob", BUY, YES, 20, 0.50);
        order(engine, 1, "Carol", BUY, YES, 15, 0.48);
        order(engine, 1, "Zoe", SELL, YES, 25, 0.58);
        stats(engine.getOrderBookState(1), YES, null, 0.50, 0.58, 0.54, 0.08, mode + "step 5 quotes");
        order(engine, 1, "Zoe", SELL, YES, 15, 0.65);

        // steps 6-7: Alice takes Zoe's 0.58 level
        OrderResultDTO r = order(engine, 1, "Alice", BUY, YES, 25, 0.58);
        expect(25L, r.filledQuantity(), mode + "Alice filled 25");
        expect(0L, r.restingQuantity(), mode + "nothing of Alice rests");
        expect(1, r.trades().size(), mode + "one resale");
        expect("Zoe", r.trades().get(0).counterpartyName(), mode + "sold by Zoe");
        near(0.58, r.trades().get(0).price(), mode + "at 0.58");
        stats(engine.getOrderBookState(1), YES, 0.58, 0.50, 0.65, 0.575, 0.15, mode + "step 8 spread widened");

        // steps 8-10: NO resale, no mint (0.50 + 0.45 < 1)
        order(engine, 1, "Zoe", SELL, NO, 50, 0.45);
        r = order(engine, 1, "Bob", BUY, NO, 25, 0.45);
        expect(1, r.trades().size(), mode + "Bob NO: a single trade");
        expectTrue(!r.trades().get(0).minted(), mode + "Bob NO is a resale, not a mint");
        asks(engine.getOrderBookState(1), NO, "Zoe:25@0.45", mode + "Zoe NO ask partially filled");

        // steps 11-12: Zoe sells into two bids
        r = order(engine, 1, "Zoe", SELL, YES, 30, 0.45);
        expect(2, r.trades().size(), mode + "the sell walks through two bids");
        expect("Bob", r.trades().get(0).buyerName(), mode + "first Bob");
        near(0.50, r.trades().get(0).price(), mode + "Bob at 0.50");
        expect(20L, r.trades().get(0).quantity(), mode + "Bob 20");
        expect("Carol", r.trades().get(1).buyerName(), mode + "then Carol");
        near(0.48, r.trades().get(1).price(), mode + "Carol at 0.48");
        expect(10L, r.trades().get(1).quantity(), mode + "Carol 10");
        bids(engine.getOrderBookState(1), YES, "Carol:5@0.48", mode + "Carol keeps 5");
        stats(engine.getOrderBookState(1), YES, 0.48, 0.48, 0.65, 0.565, 0.17, mode + "step 13 YES");

        // step 13: Carol's NO bid rests (no cross with 0.45, no mint with 0.48)
        order(engine, 1, "Carol", BUY, NO, 35, 0.42);
        bids(engine.getOrderBookState(1), NO, "Carol:35@0.42", mode + "Carol NO bid rests");

        // steps 14-15: peer-to-peer mint
        r = order(engine, 1, "Alice", BUY, YES, 40, 0.62);
        expect(2, r.trades().size(), mode + "a mint is recorded for both buyers");
        expectTrue(r.trades().get(0).minted() && r.trades().get(1).minted(), mode + "both minted");
        expect(35L, r.trades().get(0).quantity(), mode + "min(40, 35) = 35 pairs");
        near(0.58, r.trades().get(0).price(), mode + "Alice pays the complement 0.58");
        expect("Carol", r.trades().get(1).buyerName(), mode + "second trade is Carol's");
        near(0.42, r.trades().get(1).price(), mode + "Carol keeps her 0.42");
        expect(5L, r.restingQuantity(), mode + "Alice's other 5 rest");
        s = engine.getOrderBookState(1);
        bids(s, YES, "Alice:5@0.62,Carol:5@0.48", mode + "YES bids after the mint");
        bids(s, NO, "", mode + "NO bids empty after the mint");
        near(135, s.eventInfo().accountBalance(), mode + "event account 135 after the mint");
        stats(s, YES, 0.58, 0.62, 0.65, 0.635, 0.03, mode + "YES after the mint");
        stats(s, NO, 0.42, null, 0.45, null, null, mode + "NO after the mint");

        // step 16: rejected price
        expectThrows(UserInputException.class, () -> order(engine, 1, "Bob", BUY, YES, 10, 1.05),
                mode + "price above d - 0.01");

        // step 17: Bob's NO ask finds no buyer
        order(engine, 1, "Bob", SELL, NO, 25, 0.15);
        asks(engine.getOrderBookState(1), NO, "Bob:25@0.15,Zoe:25@0.45", mode + "NO asks, cheapest first");

        s = engine.getOrderBookState(1);
        shares(s, "Zoe", 45, 75, mode + "Zoe holdings");
        shares(s, "Alice", 60, 0, mode + "Alice holdings");
        shares(s, "Bob", 20, 25, mode + "Bob holdings");
        shares(s, "Carol", 10, 35, mode + "Carol holdings");

        // step 18: resolution, YES wins
        engine.closeEvent(1, "Zoe", YES);
        s = engine.getOrderBookState(1);
        if (onPurchase) {
            near(486.3055, balance(engine, "Zoe"), mode + "Zoe final");
            near(224.852, balance(engine, "Alice"), mode + "Alice final");
            near(198.5375, balance(engine, "Bob"), mode + "Bob final");
            near(190.305, balance(engine, "Carol"), mode + "Carol final");
            near(0.7555, s.totalCommissionCollected(), mode + "Zoe's fees");
            near(24.852, participant(s, "Alice").profitOrLoss(), mode + "Alice P&L = cash change");
            near(-14.45, participant(s, "Zoe").profitOrLoss(), mode + "Zoe P&L without fee income");
        } else {
            near(486.45, balance(engine, "Zoe"), mode + "Zoe final");
            near(224.6, balance(engine, "Alice"), mode + "Alice final");
            near(198.55, balance(engine, "Bob"), mode + "Bob final");
            near(190.4, balance(engine, "Carol"), mode + "Carol final");
            near(1.35, s.totalCommissionCollected(), mode + "Zoe's fees");
            near(24.6, participant(s, "Alice").profitOrLoss(), mode + "Alice P&L = cash change");
        }
        near(0, s.eventInfo().accountBalance(), mode + "event account empty");
        bids(s, YES, "", mode + "YES orders cancelled");
        asks(s, NO, "", mode + "NO orders cancelled");
        double total = 0;
        for (String name : List.of("Zoe", "Alice", "Bob", "Carol")) {
            total += balance(engine, name);
        }
        near(1100, total, mode + "money conserved");
        near(60, participant(s, "Alice").holdingValuePerOption().get(YES), mode + "winning holding value");
        near(0, participant(s, "Bob").holdingValuePerOption().get(NO), mode + "losing holding value");
        expectThrows(EventException.class, () -> order(engine, 1, "Bob", BUY, YES, 1, 0.5),
                mode + "no orders after close");
    }

    static void edgeCases() {
        GuessMarketEngine a = Scenario.clobOnPurchase();
        expectThrows(EventException.class, () -> order(a, 1, "Bob", BUY, YES, 10, 0.5),
                "order before open");
        a.openEvent(1, "Zoe");
        expectThrows(EventException.class, () -> a.buyShares(1, "Bob", 0, 10), "LMSR buy on OB");
        expectThrows(EventException.class, () -> a.getMarketState(1), "LMSR state of OB");
        a.openEvent(3, "Zoe");
        expectThrows(EventException.class, () -> order(a, 3, "Bob", BUY, YES, 10, 0.5),
                "order on LMSR");
        expectThrows(UserInputException.class, () -> order(a, 1, "Bob", BUY, YES, 10, 0.555), "fraction of cent");
        expectThrows(UserInputException.class, () -> order(a, 1, "Bob", BUY, YES, 10, 0), "price 0");
        expectThrows(UserInputException.class, () -> order(a, 1, "Bob", BUY, YES, 0, 0.5), "quantity 0");
        expectThrows(UserInputException.class,
                () -> a.submitOrder(new OrderRequestDTO(1, "Bob", null, YES, 1, 0.5)), "missing side");
        expectThrows(UserAccountException.class, () -> order(a, 1, "Carol", SELL, YES, 1, 0.5),
                "sell without shares");
        order(a, 1, "Zoe", SELL, YES, 60, 0.90);
        expectThrows(UserAccountException.class, () -> order(a, 1, "Zoe", SELL, YES, 50, 0.95),
                "sell more than held minus offered");
        order(a, 1, "Bob", BUY, NO, 5, 0.30);
        expectTrue(a.getUserDetails("Bob").events().stream().anyMatch(e -> e.event().id() == 1 && e.participant()),
                "an unmatched order makes a participant");

        GuessMarketEngine b = Scenario.clobOnPurchase();
        b.openEvent(2, "Zoe");
        order(b, 2, "Carol", BUY, NO, 35, 0.42);
        OrderResultDTO noMint = order(b, 2, "Alice", BUY, YES, 40, 0.62);
        expect(0, noMint.trades().size(), "no mint when minting is not allowed");
        expect(40L, noMint.restingQuantity(), "the whole order rests");

        GuessMarketEngine c = Scenario.clobOnPurchase();
        c.openEvent(2, "Zoe");
        order(c, 2, "Zoe", SELL, YES, 20, 0.60);
        order(c, 2, "Zoe", SELL, YES, 40, 0.60);
        OrderResultDTO walk = order(c, 2, "Bob", BUY, YES, 50, 0.60);
        expect(2, walk.trades().size(), "v3 example: two orders consumed");
        expect(20L, walk.trades().get(0).quantity(), "v3 example: 20 from the first");
        expect(30L, walk.trades().get(1).quantity(), "v3 example: 30 from the second");
        asks(c.getOrderBookState(2), YES, "Zoe:10@0.60", "v3 example: 10 left waiting");

        GuessMarketEngine d = Scenario.clobOnPurchase();
        d.openEvent(2, "Zoe");
        order(d, 2, "Dan", BUY, YES, 10, 0.30);
        order(d, 2, "Zoe", SELL, NO, 100, 0.99);
        OrderResultDTO blocking = order(d, 2, "Dan", BUY, NO, 30, 0.99);
        expectTrue(blocking.userBlocked() && blocking.userBalance() < 0, "Dan went below zero and is blocked");
        expectThrows(UserAccountException.class, () -> order(d, 2, "Dan", SELL, NO, 1, 0.5), "blocked user orders");
        bids(d.getOrderBookState(2), YES, "", "a blocked user's bid is no longer shown");
        OrderResultDTO skipped = order(d, 2, "Zoe", SELL, YES, 10, 0.10);
        expect(0, skipped.trades().size(), "a blocked user's bid is not matched");
        asks(d.getOrderBookState(2), YES, "Zoe:10@0.10", "the sell rests instead");
        OrderResultDTO eve = order(d, 2, "Eve", BUY, NO, 80, 0.99);
        expect(70L, eve.filledQuantity(), "Eve takes the 70 available");
        expectTrue(eve.userBlocked(), "Eve blocked");
        expect(0L, eve.restingQuantity(), "the rest of a blocked buyer's order does not wait");
        bids(d.getOrderBookState(2), NO, "", "no NO bid of Eve");

        GuessMarketEngine f = Scenario.clobOnPurchase();
        expectThrows(UserAccountException.class, () -> f.openEvent(4, "Dan"), "OB open without enough money");

        expectThrows(EventException.class, () -> Scenario.upload(Scenario.clobOnClose(), "Zoe",
                "odd.xml", Scenario.file(Scenario.orderBook("Odd Investment", "on-close", 1, true, 100, 3,
                        "YES", "NO"))), "initial not divisible by d");
    }

    /**
     * The rows of the accounts around an order book trade: an order that only waits moves no money, a
     * trade pays the seller and the market maker, and closing pays the holders of the winning option.
     */
    static void accountRows() {
        // Event 3: the order book of Tikva, commission of 50% on purchase, no minting.
        GuessMarketEngine engine = Scenario.multiple();
        engine.openEvent(3, "Tikva");
        int tikvaRows = engine.getAccountEntries("Tikva").size();

        order(engine, 3, "Menash", BUY, YES, 10, 0.40);
        expect(1, engine.getAccountEntries("Menash").size(), "a waiting buy order adds no row to the buyer");
        expect(tikvaRows, engine.getAccountEntries("Tikva").size(), "a waiting buy order pays no commission");

        order(engine, 3, "Tikva", SELL, YES, 10, 0.40);
        row(engine, "Menash", 0, AccountEntryType.EVENT, -6, "the buyer pays the price and the commission");
        row(engine, "Tikva", 0, AccountEntryType.COMMISSION, 2, "the market maker receives the commission");
        row(engine, "Tikva", 1, AccountEntryType.EVENT, 4, "the seller receives the price");

        engine.closeEvent(3, "Tikva", YES);
        row(engine, "Menash", 0, AccountEntryType.PAYOUT, 10, "a holder of the winning option is paid on close");
    }

    /**
     * @param index the place of the row in the account of the user, 0 for the latest one
     */
    static void row(GuessMarketEngine engine, String user, int index, AccountEntryType type, double amount,
                    String what) {
        AccountEntryDTO entry = engine.getAccountEntries(user).get(index);
        expect(type, entry.type(), what + ": the type of the row");
        near(amount, entry.amount(), what + ": the amount of the row");
    }

    static OrderResultDTO order(GuessMarketEngine engine, int eventId, String user, OrderSide side,
                                int option, long quantity, double price) {
        return engine.submitOrder(new OrderRequestDTO(eventId, user, side, option, quantity, price));
    }

    static double balance(GuessMarketEngine engine, String name) {
        for (UserInfoDTO user : engine.getAllUsers()) {
            if (user.name().equals(name)) {
                return user.balance();
            }
        }
        throw new AssertionError("no user " + name);
    }

    static OrderBookParticipantDTO participant(OrderBookStateDTO state, String name) {
        for (OrderBookParticipantDTO p : state.participants()) {
            if (p.userName().equals(name)) {
                return p;
            }
        }
        throw new AssertionError("no participant " + name);
    }

    static void shares(OrderBookStateDTO state, String name, long yes, long no, String what) {
        OrderBookParticipantDTO p = participant(state, name);
        expect(yes, p.sharesPerOption().get(YES), what + " (YES)");
        expect(no, p.sharesPerOption().get(NO), what + " (NO)");
    }

    static void stats(OrderBookStateDTO state, int option, Double last, Double bid, Double ask, Double mid,
                      Double spread, String what) {
        OrderBookOptionDTO o = state.options().get(option);
        nearOrNull(last, o.lastPrice(), what + " LAST");
        nearOrNull(bid, o.bestBid(), what + " BID");
        nearOrNull(ask, o.bestAsk(), what + " ASK");
        nearOrNull(mid, o.midPrice(), what + " MID");
        nearOrNull(spread, o.spread(), what + " SPREAD");
    }

    static void bids(OrderBookStateDTO state, int option, String expected, String what) {
        expect(expected, describe(state.options().get(option).bids()), what);
    }

    static void asks(OrderBookStateDTO state, int option, String expected, String what) {
        expect(expected, describe(state.options().get(option).asks()), what);
    }

    static String describe(List<OrderDTO> orders) {
        List<String> parts = new ArrayList<>();
        for (OrderDTO order : orders) {
            parts.add(order.userName() + ":" + order.quantity() + "@"
                    + String.format(Locale.ROOT, "%.2f", order.price()));
        }
        return String.join(",", parts);
    }
}
