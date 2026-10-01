import gm.dto.EventStatus;
import gm.dto.MarketStateDTO;
import gm.dto.PurchaseResultDTO;
import gm.dto.UserInfoDTO;
import gm.engine.exception.EventAlreadyOpenedException;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.InsufficientFundsException;
import gm.engine.exception.NotEventMarketMakerException;
import gm.engine.exception.UserBlockedException;
import gm.engine.exception.UserNotFoundException;
import gm.engine.impl.GuessMarketEngineImpl;

/**
 * The life cycle of an LMSR event and where its money goes: opening, buying with the commission on
 * purchase or on close, going below zero, closing and the payout.
 * {@link Scenario#small()}: event 1 LMSR b=100, on-purchase 5%, MM Tikva (10000). event 2 OB, MM Avrum.
 * {@link Scenario#lifecycle()}: event 1 LMSR b=100 on-close 10%, MM Poor (50); event 2 LMSR b=100
 * on-close 10%, MM Rich (10000). Users Alice (1000), Bob (5).
 */
public class LmsrLifecycleTest extends Check {

    public static void main(String[] args) {
        run("lmsr-lifecycle", LmsrLifecycleTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = Scenario.small();

        // uploaded events are INACTIVE with an empty account
        expectTrue(engine.getAllEvents().get(0).status() == EventStatus.INACTIVE, "loads INACTIVE");
        near(0, engine.getAllEvents().get(0).accountBalance(), "account empty on load");
        expect("Tikva", engine.getAllEvents().get(0).marketMakerName(), "MM name in DTO");

        // buy before open -> explaining exception
        expectThrows(EventNotActiveException.class, () -> engine.buyShares(1, "Menash", 0, 10),
                "buy before open");
        // non-MM cannot open
        expectThrows(NotEventMarketMakerException.class, () -> engine.openEvent(1, "Menash"),
                "non-MM open");
        expectThrows(UserNotFoundException.class, () -> engine.openEvent(1, "  nobody "), "unknown user");

        // MM opens (case-insensitive name): subsidy moves Tikva -> event account
        double subsidy = 100 * Math.log(2);
        engine.openEvent(1, " tikva ");
        near(10000 - subsidy, balanceOf(engine, "Tikva"), "Tikva paid subsidy");
        near(subsidy, engine.getMarketState(1).accountBalance(), "event holds subsidy");
        expectTrue(engine.getAllEvents().get(0).status() == EventStatus.ACTIVE, "ACTIVE after open");
        expectThrows(EventAlreadyOpenedException.class, () -> engine.openEvent(1, "Tikva"), "open twice");

        // on-purchase: Menash (100) buys 10 YES -> pays cost + 5% to Tikva
        double tikvaBefore = balanceOf(engine, "Tikva");
        PurchaseResultDTO p = engine.buyShares(1, "Menash", 0, 10);
        near(p.sharesCost() * 0.05, p.commissionPaid(), "on-purchase commission 5%");
        near(100 - p.totalPaid(), balanceOf(engine, "Menash"), "Menash paid total");
        near(tikvaBefore + p.commissionPaid(), balanceOf(engine, "Tikva"), "commission to MM");
        near(subsidy + p.sharesCost(), engine.getMarketState(1).accountBalance(),
                "event account = subsidy + shares cost only");
        expectFalse(p.buyerBlocked(), "Menash not blocked");

        // overdraft: Menash buys a lot -> goes through, gets blocked, then blocked from buying
        PurchaseResultDTO big = engine.buyShares(1, "Menash", 1, 500);
        expectTrue(big.buyerBalance() < 0 && big.buyerBlocked(), "overdraft allowed + blocked");
        expectThrows(UserBlockedException.class, () -> engine.buyShares(1, "Menash", 0, 1),
                "blocked user cannot buy");

        // close by non-MM refused; close by MM: NO (index 1) wins
        expectThrows(NotEventMarketMakerException.class, () -> engine.closeEvent(1, "Avrum", 1),
                "non-MM close");
        double menashBefore = balanceOf(engine, "Menash");
        double tikvaBeforeClose = balanceOf(engine, "Tikva");
        double accountBeforeClose = engine.getMarketState(1).accountBalance();
        engine.closeEvent(1, "Tikva", 1);
        MarketStateDTO closed = engine.getMarketState(1);
        // on-purchase event: winners get the full payout (500 shares), leftover -> MM, account 0
        near(menashBefore + 500, balanceOf(engine, "Menash"), "blocked winner still paid");
        near(tikvaBeforeClose + (accountBeforeClose - 500), balanceOf(engine, "Tikva"),
                "leftover subsidy to MM");
        near(0, closed.accountBalance(), "event account emptied");
        expectTrue(accountBeforeClose - 500 >= 0, "LMSR leftover is non-negative");
        expect("No way !", closed.winningOptionName(), "winner recorded");
        expectThrows(EventNotActiveException.class, () -> engine.closeEvent(1, "Tikva", 0), "close twice");
        expectThrows(EventAlreadyOpenedException.class, () -> engine.openEvent(1, "Tikva"), "reopen closed");

        // money is conserved across users + event account (commission and leftover only move)
        double total = 0;
        for (UserInfoDTO u : engine.getAllUsers()) {
            total += u.balance();
        }
        near(1000 + 10000 + 100, total + closed.accountBalance(), "money conserved (event 1)");

        // on-close commission + insufficient funds, on a second system
        GuessMarketEngineImpl engine2 = Scenario.lifecycle();
        expectThrows(InsufficientFundsException.class, () -> engine2.openEvent(1, "Poor"),
                "MM without enough money");
        expectTrue(engine2.getAllEvents().get(0).status() == EventStatus.INACTIVE,
                "stays INACTIVE after failed open");
        near(50, balanceOf(engine2, "Poor"), "no money moved on failed open");
        expectFalse(userOf(engine2, "Poor").blocked(), "failed open does not block");

        engine2.openEvent(2, "Rich");
        PurchaseResultDTO a = engine2.buyShares(2, "Alice", 0, 40);
        PurchaseResultDTO b = engine2.buyShares(2, "Alice", 0, 20);
        PurchaseResultDTO c = engine2.buyShares(2, "Rich", 0, 30);   // MM trades in own event
        near(0, a.commissionPaid() + b.commissionPaid() + c.commissionPaid(),
                "no commission on purchase for on-close event");
        double aliceBefore = balanceOf(engine2, "Alice");
        double richBefore = balanceOf(engine2, "Rich");
        double account2 = engine2.getMarketState(2).accountBalance();
        engine2.closeEvent(2, "Rich", 0);
        MarketStateDTO closed2 = engine2.getMarketState(2);
        // Alice holds 60 -> 60 payout, 10% = 6 to Rich. Rich holds 30 -> 27 to Rich + 3 to Rich.
        near(aliceBefore + 54, balanceOf(engine2, "Alice"), "winner gets payout minus 10%");
        double leftover = account2 - 90;
        near(richBefore + 27 + 3 + 6 + leftover, balanceOf(engine2, "Rich"),
                "MM gets own payout + all commission + leftover");
        near(9, closed2.totalCommissionCollected(), "commission total recorded");
        near(0, closed2.accountBalance(), "event account emptied");
    }

    static double balanceOf(GuessMarketEngineImpl engine, String name) {
        return userOf(engine, name).balance();
    }

    static UserInfoDTO userOf(GuessMarketEngineImpl engine, String name) {
        for (UserInfoDTO u : engine.getAllUsers()) {
            if (u.name().equalsIgnoreCase(name)) {
                return u;
            }
        }
        throw new AssertionError("no user " + name);
    }
}
