import gm.dto.PurchaseResultDTO;
import gm.engine.exception.EventNotActiveException;
import gm.engine.exception.UserBlockedException;
import gm.engine.impl.GuessMarketEngineImpl;

/**
 * The price of a purchase before it is made: it changes nothing, it is the price the purchase then has,
 * and it tells whether the purchase would leave the buyer below zero.
 * {@link Scenario#small()}: event 1 LMSR b=100, on-purchase 5%, MM Tikva (10000); Menash has 100.
 */
public class QuoteTest extends Check {

    public static void main(String[] args) {
        run("quote", QuoteTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = Scenario.small();
        expectThrows(EventNotActiveException.class, () -> engine.quoteShares(1, "Menash", 0, 10),
                "no quote for an event that is not open");
        engine.openEvent(1, "Tikva");

        // a quote changes nothing
        double balance = engine.getUserDetails("Menash").balance();
        String stateBefore = engine.getMarketState(1).toString();
        int tradesBefore = engine.getMarketState(1).tradeHistory().size();
        PurchaseResultDTO quote = engine.quoteShares(1, "Menash", 0, 10);
        expect(stateBefore, engine.getMarketState(1).toString(), "the market is as it was after a quote");
        expect(tradesBefore, engine.getMarketState(1).tradeHistory().size(), "no trade is recorded by a quote");
        near(balance, engine.getUserDetails("Menash").balance(), "no money is paid for a quote");
        near(balance - quote.totalPaid(), quote.buyerBalance(), "the quote shows the balance that is left");
        expect(false, quote.buyerBlocked(), "a small purchase does not block");

        // the purchase then costs what the quote said
        PurchaseResultDTO bought = engine.buyShares(1, "Menash", 0, 10);
        near(quote.sharesCost(), bought.sharesCost(), "the shares cost as quoted");
        near(quote.commissionPaid(), bought.commissionPaid(), "the commission as quoted");
        near(quote.totalPaid(), bought.totalPaid(), "the total as quoted");
        near(quote.buyerBalance(), bought.buyerBalance(), "the balance left as quoted");

        // a purchase that would end below zero says so, before the buyer is blocked
        PurchaseResultDTO big = engine.quoteShares(1, "Menash", 0, 1000);
        expect(true, big.buyerBlocked(), "a quote that ends below zero says the buyer would be blocked");
        expect(false, engine.getUserDetails("Menash").blocked(), "the buyer is not blocked by a quote");

        // a blocked user gets no quote, as no purchase
        engine.buyShares(1, "Menash", 0, 1000);
        expectThrows(UserBlockedException.class, () -> engine.quoteShares(1, "Menash", 0, 1),
                "no quote for a blocked user");
    }
}
