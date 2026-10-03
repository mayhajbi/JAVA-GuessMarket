import gm.dto.AccountEntryDTO;
import gm.dto.AccountEntryType;
import gm.dto.UserInfoDTO;
import gm.engine.core.Account;
import gm.engine.exception.UserInputException;
import gm.engine.exception.InvalidCommissionException;
import gm.engine.impl.GuessMarketEngineImpl;

import gm.dto.EventInfoDTO;
import gm.dto.UploadResultDTO;
import gm.engine.exception.InvalidFilePathException;
import gm.engine.exception.InvalidLiquidityException;
import gm.engine.exception.InvalidOptionsException;
import gm.engine.exception.UnsupportedFileFormatException;
import gm.engine.exception.UserNotFoundException;
import gm.engine.exception.XmlParsingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An engine that starts empty, users that register by name and load money into their account, the
 * entries of the account, a block that lasts only while the balance is below zero, and the upload of
 * event files: they pile up, the uploader is the market maker, and a bad file changes nothing.
 */
public class UsersAndUploadsTest extends Check {

    public static void main(String[] args) {
        run("users-and-uploads", UsersAndUploadsTest::check);
    }

    static void check() throws IOException {
        // The engine starts empty and needs no file.
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        expect(0, engine.getAllEvents().size(), "an engine that starts empty has no events");
        expect(0, engine.getAllUsers().size(), "an engine that starts empty has no users");

        // Registering: an empty account, a clean name, and no second user of the same name.
        UserInfoDTO dana = engine.registerUser("  Dana   Levi ");
        expect("Dana Levi", dana.name(), "the name is cleaned");
        money(0, dana.balance(), "a new user starts with an empty account");
        expectFalse(dana.blocked(), "an empty account is not blocked");
        expectFalse(dana.marketMaker(), "a new user is not a market maker");
        expectThrows(UserInputException.class, () -> engine.registerUser("dana levi"),
                "the same name in another case");
        expectThrows(UserInputException.class, () -> engine.registerUser("   "), "a blank name");
        expectThrows(UserInputException.class, () -> engine.registerUser("\u05E9\u05DC\u05D5\u05DD"),
                "a name that is not in English");
        expect(1, engine.getAllUsers().size(), "failed registrations add nobody");

        // Depositing: positive amounts only, and every one is an entry, latest first.
        money(100, engine.deposit("Dana Levi", 100).balance(), "the balance after a deposit");
        money(150.5, engine.deposit("dana levi", 50.5).balance(), "the name is found without case");
        expectThrows(UserInputException.class, () -> engine.deposit("Dana Levi", 0), "a zero deposit");
        expectThrows(UserInputException.class, () -> engine.deposit("Dana Levi", -5),
                "a negative deposit");
        expectThrows(UserInputException.class, () -> engine.deposit("Dana Levi", Double.NaN),
                "a deposit that is not a number");
        List<AccountEntryDTO> entries = engine.getAccountEntries("Dana Levi");
        expect(2, entries.size(), "only the accepted deposits are entries");
        expect(AccountEntryType.DEPOSIT, entries.get(0).type(), "the kind of an entry");
        money(50.5, entries.get(0).amount(), "the latest entry comes first");
        money(150.5, entries.get(0).balanceAfter(), "the balance after the latest entry");
        money(100, entries.get(1).balanceAfter(), "the balance after the first entry");

        // The block lasts only while the balance is below zero: 50 - 80 = -30, then +100 = 70.
        Account account = new Account(50);
        expectFalse(account.isBlocked(), "not blocked with 50");
        account.withdraw(80, AccountEntryType.EVENT);
        expectTrue(account.isBlocked(), "blocked with -30");
        account.deposit(30, AccountEntryType.PAYOUT);
        expectFalse(account.isBlocked(), "not blocked with exactly 0");
        account.withdraw(1, AccountEntryType.EVENT);
        expectTrue(account.isBlocked(), "blocked again with -1");
        account.deposit(100, AccountEntryType.DEPOSIT);
        expectFalse(account.isBlocked(), "not blocked with 99");
        money(99, account.getBalance(), "the balance at the end");
        expect(4, account.getEntries().size(), "every movement is an entry");
        expect(5, account.getBalanceHistory().size(), "the history is the opening balance and the entries");

        uploads();
    }

    /** Files of events pile up, the uploader is their market maker, and a bad file changes nothing. */
    static void uploads() throws IOException {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.registerUser("Avi");
        engine.registerUser("Bella");

        UploadResultDTO first = upload(engine, "Avi", DATA + "ex3/small.xml");
        expect("[Mujtaba is Dead]", first.eventNames().toString(), "the events the first file added");
        UploadResultDTO second = upload(engine, "Avi", DATA + "ex3/multiple.xml");
        expect(3, second.eventNames().size(), "the events the second file added");
        List<EventInfoDTO> events = engine.getAllEvents();
        expect(4, events.size(), "the files pile up instead of replacing each other");
        expect("[1, 2, 3, 4]", events.stream().map(EventInfoDTO::id).toList().toString(),
                "the ids follow each other across files");
        expect("Avi", events.get(3).marketMakerName(), "the uploader is the market maker");
        expectTrue(user(engine, "Avi").marketMaker(), "the uploader is shown as a market maker");
        expectFalse(user(engine, "Bella").marketMaker(), "another user is not");

        // A name that exists is refused, whatever its case, and so is a name twice in one file.
        expectThrows(UserInputException.class,
                () -> upload(engine, "Bella", DATA + "ex3/small.xml"), "the same file again");
        expectThrows(UserInputException.class,
                () -> Scenario.upload(engine, "Bella", "again.xml", Scenario.lmsrFile("Fresh one", "MUJTABA IS DEAD")),
                "a name that exists in another case, after a fresh event");
        expectThrows(UserInputException.class,
                () -> Scenario.upload(engine, "Bella", "twice.xml", Scenario.lmsrFile("Twin", "twin")),
                "the same name twice in one file");
        expect(4, engine.getAllEvents().size(), "a refused file adds no event at all");

        // A file that is not acceptable says why, and changes nothing.
        expectThrows(UnsupportedFileFormatException.class,
                () -> Scenario.upload(engine, "Bella", "users.xml",
                        "<Guess-Market><GM-events/><GM-users/></Guess-Market>"), "a file with users");
        expectThrows(UnsupportedFileFormatException.class,
                () -> Scenario.upload(engine, "Bella", "ids.xml",
                        Scenario.lmsrFile("Numbered").replace("<description>", "<id>7</id><description>")),
                "a file with event ids");
        expectThrows(InvalidFilePathException.class,
                () -> Scenario.upload(engine, "Bella", "events.txt", Scenario.lmsrFile("Fine")), "a file that is not xml");
        expectThrows(XmlParsingException.class,
                () -> Scenario.upload(engine, "Bella", "broken.xml", "<Guess-Market><GM-events>"), "a broken file");
        expectThrows(UserNotFoundException.class,
                () -> Scenario.upload(engine, "Nobody", "fine.xml", Scenario.lmsrFile("Fine")), "an unknown uploader");
        expect(4, engine.getAllEvents().size(), "the refused files added nothing");

        String badCommission = Scenario.lmsrFile("Costly").replace(">5</commission>", ">95</commission>");
        expectThrows(InvalidCommissionException.class,
                () -> Scenario.upload(engine, "Bella", "costly.xml", badCommission), "a commission above the limit");

        // The input checks of exercise 1 apply to an uploaded file too: a liquidity of zero, and one option.
        String zeroLiquidity = Scenario.lmsrFile("Flat").replace("<b>100</b>", "<b>0</b>");
        expectThrows(InvalidLiquidityException.class,
                () -> Scenario.upload(engine, "Bella", "flat.xml", zeroLiquidity), "a liquidity of zero");
        String singleOption = Scenario.lmsrFile("Lonely").replace("<GM-option>No</GM-option>", "");
        expectThrows(InvalidOptionsException.class,
                () -> Scenario.upload(engine, "Bella", "lonely.xml", singleOption), "an event with one option");
        expect(4, engine.getAllEvents().size(), "the refused files added nothing, again");

        // Only English is accepted: in the name, the description and the options of an event.
        expectThrows(UserInputException.class,
                () -> Scenario.upload(engine, "Bella", "hebrew.xml", Scenario.lmsrFile("\u05E9\u05DC\u05D5\u05DD")),
                "an event name that is not in English");
        String foreignOption = Scenario.lmsrFile("Foreign").replace("<GM-option>No</GM-option>",
                "<GM-option>\u05DC\u05D0</GM-option>");
        expectThrows(UserInputException.class,
                () -> Scenario.upload(engine, "Bella", "foreign.xml", foreignOption),
                "an option that is not in English");
        expect(4, engine.getAllEvents().size(), "the refused files added nothing, once more");

        // A file of another user is another market maker, and a name that is free is accepted.
        UploadResultDTO third = Scenario.upload(engine, "Bella", "bella.xml", Scenario.lmsrFile("Bella's event"));
        expect("[Bella's event]", third.eventNames().toString(), "a new name is accepted");
        expect("Bella", engine.getAllEvents().get(4).marketMakerName(), "the second uploader is its maker");
        expect(5, engine.getAllEvents().get(4).id(), "the id continues");
    }

    static UploadResultDTO upload(GuessMarketEngineImpl engine, String user, String path) {
        try (InputStream content = new FileInputStream(path)) {
            return engine.uploadEvents(user, new File(path).getName(), content);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    static UserInfoDTO user(GuessMarketEngineImpl engine, String name) {
        return engine.getAllUsers().stream().filter(user -> user.name().equals(name)).findFirst().orElseThrow();
    }
}
