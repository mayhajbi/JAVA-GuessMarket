import gm.dto.OrderRequestDTO;
import gm.dto.OrderSide;
import gm.engine.exception.GuessMarketException;
import gm.engine.impl.GuessMarketEngineImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * The wording of the messages a user gets when something is refused: no brackets of any kind, no letter
 * of a variable in a trading message, money with two digits after the point, and the name of the XML
 * element or attribute in a message about an uploaded file.
 */
public class MessageWordingTest extends Check {

    private static final String BRACKETS = "()[]<>";
    private static final String EARTH_QUAKE = "Earth Quake on Dead Sea";
    private static final int ORDER_BOOK_ID = 1;
    private static final int LMSR_ID = 3;

    /** Every message the check saw, to look for brackets in all of them at the end. */
    private static final List<String> messages = new ArrayList<>();

    public static void main(String[] args) {
        run("message-wording", MessageWordingTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.registerUser("Avi");
        engine.registerUser("Ben");
        UsersAndUploadsTest.upload(engine, "Avi", DATA + "ex3/multiple.xml");

        // money
        expect("Enter an amount greater than zero. -5.00 cannot be loaded.",
                messageOf(() -> engine.deposit("Avi", -5)), "a negative deposit");
        engine.deposit("Avi", 50);
        expect("You cannot open 'Will it rain tomorrow ?'. Opening it costs 138.63 for the initial subsidy, "
                        + "but your balance is 50.00. Load funds and try again.",
                messageOf(() -> engine.openEvent(LMSR_ID, "Avi")), "opening without enough money");
        expectTrue(messageOf(() -> engine.openEvent(ORDER_BOOK_ID, "Avi")).contains("costs 1000.00 for the initial investment"),
                "opening an order book event without enough money names the initial investment");

        // trading
        engine.deposit("Avi", 2000);
        engine.openEvent(ORDER_BOOK_ID, "Avi");
        engine.openEvent(LMSR_ID, "Avi");
        expect("A price of 10.00 is too high for '" + EARTH_QUAKE + "'. A winning share pays 1.00, so the "
                        + "price must be between 0.01 and 0.99.",
                messageOf(() -> order(engine, 10)), "a price above the range");
        expectTrue(messageOf(() -> order(engine, 0)).startsWith("A price of 0.00 is too low for"),
                "a price below the range");
        expectTrue(messageOf(() -> order(engine, 0.555)).startsWith("The price 0.555 has more than two decimal places"),
                "a price with a fraction of a cent is shown as it was given");
        expect("'Will it rain tomorrow ?' has no option number 6. Choose an option from 1 to 2.",
                messageOf(() -> engine.buyShares(LMSR_ID, "Ben", 5, 1)), "an option that does not exist");
        messageOf(() -> engine.buyShares(LMSR_ID, "Ben", 0, 0));
        messageOf(() -> engine.buyShares(999, "Ben", 0, 1));
        messageOf(() -> engine.buyShares(ORDER_BOOK_ID, "Ben", 0, 1));
        messageOf(() -> engine.openEvent(LMSR_ID, "Ben"));
        messageOf(() -> engine.openEvent(LMSR_ID, "Avi"));
        messageOf(() -> engine.submitOrder(null));
        messageOf(() -> engine.submitOrder(new OrderRequestDTO(ORDER_BOOK_ID, "Ben", OrderSide.SELL, 0, 5, 0.5)));
        engine.buyShares(LMSR_ID, "Ben", 0, 10);
        expect("You cannot buy shares, because your balance is below zero and you are blocked. Load funds "
                        + "until your balance is zero or more.",
                messageOf(() -> engine.buyShares(LMSR_ID, "Ben", 0, 1)), "a blocked user");

        // uploaded files
        expectTrue(messageOf(() -> UsersAndUploadsTest.upload(engine, "Ben", DATA + "ex3/multiple.xml"))
                .endsWith("Names are not case-sensitive. Choose a different name."), "an event name that is in use");
        expect("The attribute 'd' of the element 'GM-order-book' is missing or empty in the event 'No d'. Add "
                        + "it and upload the file again.",
                messageOf(() -> uploadOrderBook(engine, "No d", "allow-mint=\"true\" initial=\"10\"")),
                "a missing attribute");
        expectTrue(messageOf(() -> uploadOrderBook(engine, "Zero d", "allow-mint=\"true\" initial=\"10\" d=\"0\""))
                .endsWith("In a file, it is the attribute 'd'."), "a base value of zero names the attribute");
        expectTrue(messageOf(() -> uploadOrderBook(engine, "Odd", "allow-mint=\"true\" initial=\"10\" d=\"3\""))
                .contains("must be a multiple of the base value 3."), "an initial investment that does not divide");
        expectTrue(messageOf(() -> uploadOrderBook(engine, "Minus", "allow-mint=\"true\" initial=\"-3\" d=\"1\""))
                .endsWith("In a file, it is the attribute 'initial'."), "a negative initial investment");
        expectTrue(messageOf(() -> uploadOrderBook(engine, "Mint", "allow-mint=\"maybe\" initial=\"10\" d=\"1\""))
                .startsWith("'maybe' is not valid for the attribute 'allow-mint' of 'Mint'."),
                "an allow-mint value that is not a boolean");
        expectTrue(messageOf(() -> upload(engine, "flat.xml", lmsr("Flat", "on-close", 5, 0, "Yes", "No")))
                .endsWith("In a file, it is the element 'b'."), "a liquidity of zero names the element");
        expectTrue(messageOf(() -> upload(engine, "weekly.xml", lmsr("Weekly", "weekly", 5, 100, "Yes", "No")))
                .endsWith("use 'on-purchase' or 'on-close'."), "an unknown commission type");
        expectTrue(messageOf(() -> upload(engine, "costly.xml", lmsr("Costly", "on-close", 95, 100, "Yes", "No")))
                .endsWith("It must be a whole number from 0 to 90."), "a commission above the limit");
        expect("'Lonely' has 1 options. An event must have exactly 2, each in a 'GM-option' element.",
                messageOf(() -> upload(engine, "lonely.xml", lmsr("Lonely", "on-close", 5, 100, "Yes"))),
                "an event with one option");
        expectTrue(messageOf(() -> upload(engine, "users.xml",
                        "<Guess-Market><GM-events/><GM-users/></Guess-Market>"))
                .startsWith("The file cannot be used, because the file has the element 'GM-users'."),
                "a file with users");
        expect("The file cannot be used, because one of the events of the file has the element 'id'. Events "
                        + "are identified by their names. Remove it and upload the file again.",
                messageOf(() -> upload(engine, "ids.xml", "<Guess-Market><GM-events><GM-event><id>7</id>"
                        + "</GM-event></GM-events></Guess-Market>")), "an event with an id and without a name");
        expectTrue(messageOf(() -> upload(engine, "empty.xml", "<Guess-Market><GM-events/></Guess-Market>"))
                .startsWith("The element 'GM-event' is missing or empty in the element 'GM-events'."),
                "a file without events");
        expectTrue(messageOf(() -> upload(engine, "none.xml", "<Guess-Market/>"))
                .startsWith("The element 'GM-events' is missing or empty in the root element 'Guess-Market'."),
                "a file without the events element");
        expect("The file 'events.txt' is not an XML file. The file name must end with the .xml extension.",
                messageOf(() -> upload(engine, "events.txt", Scenario.lmsrFile("Fine"))),
                "a file that is not xml");
        messageOf(() -> upload(engine, "", Scenario.lmsrFile("Fine")));
        messageOf(() -> upload(engine, "same.xml", Scenario.lmsrFile("Twice", "twice")));

        for (String message : messages) {
            expectTrue(message.chars().noneMatch(character -> BRACKETS.indexOf(character) >= 0),
                    "no brackets in: " + message);
        }
    }

    /** The message of the refusal the action ends with; a check fails when nothing is refused. */
    private static String messageOf(Runnable action) {
        try {
            action.run();
        } catch (GuessMarketException refused) {
            messages.add(refused.getMessage());
            return refused.getMessage();
        }
        expectTrue(false, "the action was expected to be refused");
        return "";
    }

    private static void order(GuessMarketEngineImpl engine, double price) {
        engine.submitOrder(new OrderRequestDTO(ORDER_BOOK_ID, "Avi", OrderSide.BUY, 0, 1, price));
    }

    private static void upload(GuessMarketEngineImpl engine, String fileName, String xml) {
        Scenario.upload(engine, "Avi", fileName, xml);
    }

    private static void uploadOrderBook(GuessMarketEngineImpl engine, String name, String attributes) {
        upload(engine, "book.xml", Scenario.file(
                Scenario.event(name, "on-close", 5, "<GM-order-book " + attributes + "/>", "Yes", "No")));
    }

    /** A file with one LMSR event. */
    private static String lmsr(String name, String commissionType, int commission, int liquidity,
                               String... options) {
        return Scenario.file(Scenario.lmsr(name, commissionType, commission, liquidity, options));
    }
}
