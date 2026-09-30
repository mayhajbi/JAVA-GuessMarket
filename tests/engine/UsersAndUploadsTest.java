import gm.dto.AccountEntryDTO;
import gm.dto.AccountEntryType;
import gm.dto.UserInfoDTO;
import gm.engine.core.Account;
import gm.engine.exception.DuplicateUserNameException;
import gm.engine.exception.GuessMarketException;
import gm.engine.exception.InvalidCommissionException;
import gm.engine.exception.InvalidDepositException;
import gm.engine.exception.InvalidUserNameException;
import gm.engine.impl.GuessMarketEngineImpl;

import gm.dto.EventInfoDTO;
import gm.dto.UploadResultDTO;
import gm.engine.exception.DuplicateEventNameException;
import gm.engine.exception.InvalidFilePathException;
import gm.engine.exception.UnsupportedFileFormatException;
import gm.engine.exception.UserNotFoundException;
import gm.engine.exception.XmlParsingException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        expectThrows(DuplicateUserNameException.class, () -> engine.registerUser("dana levi"),
                "the same name in another case");
        expectThrows(InvalidUserNameException.class, () -> engine.registerUser("   "), "a blank name");
        expect(1, engine.getAllUsers().size(), "failed registrations add nobody");

        // Depositing: positive amounts only, and every one is an entry, latest first.
        money(100, engine.deposit("Dana Levi", 100).balance(), "the balance after a deposit");
        money(150.5, engine.deposit("dana levi", 50.5).balance(), "the name is found without case");
        expectThrows(InvalidDepositException.class, () -> engine.deposit("Dana Levi", 0), "a zero deposit");
        expectThrows(InvalidDepositException.class, () -> engine.deposit("Dana Levi", -5),
                "a negative deposit");
        expectThrows(InvalidDepositException.class, () -> engine.deposit("Dana Levi", Double.NaN),
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
        expectThrows(DuplicateEventNameException.class,
                () -> upload(engine, "Bella", DATA + "ex3/small.xml"), "the same file again");
        expectThrows(DuplicateEventNameException.class,
                () -> uploadText(engine, "Bella", "again.xml", eventsXml("Fresh one", "MUJTABA IS DEAD")),
                "a name that exists in another case, after a fresh event");
        expectThrows(DuplicateEventNameException.class,
                () -> uploadText(engine, "Bella", "twice.xml", eventsXml("Twin", "twin")),
                "the same name twice in one file");
        expect(4, engine.getAllEvents().size(), "a refused file adds no event at all");

        // A file that is not acceptable says why, and changes nothing.
        expectThrows(UnsupportedFileFormatException.class,
                () -> upload(engine, "Bella", DATA + "ex2/multiple.xml"), "a file with users and ids");
        expectThrows(InvalidFilePathException.class,
                () -> uploadText(engine, "Bella", "events.txt", eventsXml("Fine")), "a file that is not xml");
        expectThrows(XmlParsingException.class,
                () -> uploadText(engine, "Bella", "broken.xml", "<Guess-Market><GM-events>"), "a broken file");
        expectThrows(UserNotFoundException.class,
                () -> uploadText(engine, "Nobody", "fine.xml", eventsXml("Fine")), "an unknown uploader");
        expect(4, engine.getAllEvents().size(), "the refused files added nothing");

        // A fault in an event of a file names the event, but shows no id: the file has none.
        String badCommission = eventsXml("Costly").replace(">5</commission>", ">95</commission>");
        expectThrows(InvalidCommissionException.class,
                () -> uploadText(engine, "Bella", "costly.xml", badCommission), "a commission above the limit");
        try {
            uploadText(engine, "Bella", "costly.xml", badCommission);
        } catch (GuessMarketException exception) {
            expectFalse(exception.getMessage().contains("(id"), "the message shows no event id");
        }

        // A file of another user is another market maker, and a name that is free is accepted.
        UploadResultDTO third = uploadText(engine, "Bella", "bella.xml", eventsXml("Bella's event"));
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

    static UploadResultDTO uploadText(GuessMarketEngineImpl engine, String user, String fileName, String xml) {
        return engine.uploadEvents(user, fileName, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    static UserInfoDTO user(GuessMarketEngineImpl engine, String name) {
        return engine.getAllUsers().stream().filter(user -> user.name().equals(name)).findFirst().orElseThrow();
    }

    /** A file with one lmsr event for every given name. */
    static String eventsXml(String... names) {
        StringBuilder xml = new StringBuilder("<Guess-Market><GM-events>");
        for (String name : names) {
            xml.append("<GM-event name=\"").append(name).append("\"><description>d</description>")
                    .append("<commission type=\"on-close\">5</commission>")
                    .append("<GM-options><GM-option>Yes</GM-option><GM-option>No</GM-option></GM-options>")
                    .append("<GM-method><GM-LMSR><b>100</b></GM-LMSR></GM-method></GM-event>");
        }
        return xml.append("</GM-events></Guess-Market>").toString();
    }
}
