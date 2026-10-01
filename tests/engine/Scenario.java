import gm.dto.UploadResultDTO;
import gm.engine.impl.GuessMarketEngineImpl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Prepares an engine for a check the way the users of the system do: they register, load money and
 * upload files of events. Every event is uploaded in a file of its own, so the events get the ids
 * 1, 2, 3... in the order they are given here.
 */
final class Scenario {

    private final GuessMarketEngineImpl engine = new GuessMarketEngineImpl();

    private Scenario() {
    }

    /** Avrum 1000, Tikva 10000, Menash 100; events 1 and 2. */
    static GuessMarketEngineImpl small() {
        return threeUsers()
                .event("Tikva", lmsr("Mujtaba is Dead", "on-purchase", 5, 100, "Hell Yea !", "No way !"))
                .event("Avrum", orderBook("World Cap Winner", "on-close", 15, true, 100, 1, "Argentina", "Spain"))
                .engine;
    }

    /**
     * Avrum 1000, Tikva 10000, Menash 100; 1 LMSR on-purchase (Tikva), 2 order book on-close with
     * minting (Avrum), 3 order book on-purchase (Tikva), 4 LMSR on-close (Tikva).
     */
    static GuessMarketEngineImpl multiple() {
        return threeUsers()
                .event("Tikva", lmsr("Mujtaba is Dead", "on-purchase", 5, 100, "Hell Yea !", "No way !"))
                .event("Avrum", orderBook("World Cap Winner", "on-close", 10, true, 100, 1, "Argentina", "Spain"))
                .event("Tikva", orderBook("Earth Quake on Dead Sea", "on-purchase", 50, false, 1000, 1, "Yes", "No"))
                .event("Tikva", lmsr("Will it rain tomorrow ?", "on-close", 10, 200, "Yes", "No"))
                .engine;
    }

    /** Poor 50, Rich 10000, Alice 1000, Bob 5; two LMSR events, on-close 10%: 1 of Poor, 2 of Rich. */
    static GuessMarketEngineImpl lifecycle() {
        return new Scenario().user("Poor", 50).user("Rich", 10000).user("Alice", 1000).user("Bob", 5)
                .event("Poor", lmsr("Poor MM Event", "on-close", 10, 100, "Yes", "No"))
                .event("Rich", lmsr("Rich MM Event", "on-close", 10, 100, "Yes", "No"))
                .engine;
    }

    /**
     * The market of clob_simulation.html, commission on close: Zoe 500 and Alice, Bob and Carol 200
     * each; event 1 is the order book of Zoe.
     */
    static GuessMarketEngineImpl clobOnClose() {
        return clobUsers()
                .event("Zoe", orderBook("Will it rain tomorrow?", "on-close", 1, true, 100, 1, "YES", "NO"))
                .engine;
    }

    /**
     * The market of clob_simulation.html, commission on purchase, with more to check the edge cases
     * on: Dan 20 and Eve 10 as well; 1 the order book of the simulation, 2 an order book without
     * minting, 3 an LMSR event (all of Zoe), 4 an order book Dan cannot afford.
     */
    static GuessMarketEngineImpl clobOnPurchase() {
        return clobUsers().user("Dan", 20).user("Eve", 10)
                .event("Zoe", orderBook("Will it rain tomorrow?", "on-purchase", 1, true, 100, 1, "YES", "NO"))
                .event("Zoe", orderBook("No Mint Market", "on-purchase", 1, false, 100, 1, "YES", "NO"))
                .event("Zoe", lmsr("LMSR Market", "on-purchase", 1, 100, "YES", "NO"))
                .event("Dan", orderBook("Expensive Market", "on-close", 1, true, 1000, 1, "YES", "NO"))
                .engine;
    }

    /** Uploads a file under the given name, on behalf of the user. */
    static UploadResultDTO upload(GuessMarketEngineImpl engine, String user, String fileName, String xml) {
        return engine.uploadEvents(user, fileName, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** A file with one plain LMSR event for every given name. */
    static String lmsrFile(String... names) {
        return file(Arrays.stream(names).map(name -> lmsr(name, "on-close", 5, 100, "Yes", "No"))
                .toArray(String[]::new));
    }

    /** A file that holds the given events. */
    static String file(String... events) {
        return "<Guess-Market><GM-events>" + String.join("", events) + "</GM-events></Guess-Market>";
    }

    static String lmsr(String name, String commissionType, int commission, int liquidity, String... options) {
        return event(name, commissionType, commission, "<GM-LMSR><b>" + liquidity + "</b></GM-LMSR>", options);
    }

    static String orderBook(String name, String commissionType, int commission, boolean allowMint, int initial,
                            int baseValue, String... options) {
        return event(name, commissionType, commission, "<GM-order-book allow-mint=\"" + allowMint
                + "\" initial=\"" + initial + "\" d=\"" + baseValue + "\"/>", options);
    }

    /**
     * @param method the content of the GM-method element, as it is written in a file
     */
    static String event(String name, String commissionType, int commission, String method, String... options) {
        StringBuilder xml = new StringBuilder("<GM-event name=\"" + name + "\"><description>About " + name
                + "</description><commission type=\"" + commissionType + "\">" + commission
                + "</commission><GM-options>");
        for (String option : options) {
            xml.append("<GM-option>").append(option).append("</GM-option>");
        }
        return xml.append("</GM-options><GM-method>").append(method).append("</GM-method></GM-event>").toString();
    }

    private static Scenario threeUsers() {
        return new Scenario().user("Avrum", 1000).user("Tikva", 10000).user("Menash", 100);
    }

    private static Scenario clobUsers() {
        return new Scenario().user("Zoe", 500).user("Alice", 200).user("Bob", 200).user("Carol", 200);
    }

    private Scenario user(String name, double cash) {
        engine.registerUser(name);
        engine.deposit(name, cash);
        return this;
    }

    private Scenario event(String marketMaker, String eventXml) {
        upload(engine, marketMaker, "event.xml", file(eventXml));
        return this;
    }
}
