import gm.dto.ChatLineDTO;
import gm.dto.ChatLinesDTO;
import gm.engine.exception.InvalidChatLineException;
import gm.engine.exception.UserNotFoundException;
import gm.engine.impl.GuessMarketEngineImpl;

/**
 * The chat of the users (bonus): the lines pile up in the order they were written, a caller that knows
 * a version gets only what came after it, and a line that is empty or not in English is refused.
 */
public class ChatTest extends Check {

    public static void main(String[] args) {
        run("chat", ChatTest::check);
    }

    static void check() {
        GuessMarketEngineImpl engine = new GuessMarketEngineImpl();
        engine.registerUser("Avi");
        engine.registerUser("Bella");

        ChatLinesDTO empty = engine.getChatLines(0);
        expect(0, empty.version(), "a chat nobody wrote in is at version 0");
        expect(0, empty.lines().size(), "a chat nobody wrote in has no lines");

        long before = System.currentTimeMillis();
        engine.sendChatLine("avi", "  Hello \n everyone  ");
        engine.sendChatLine("Bella", "Hi Avi");
        ChatLinesDTO both = engine.getChatLines(0);
        expect(2, both.version(), "the version is the amount of lines");
        expect("[Avi: Hello everyone, Bella: Hi Avi]", describe(both), "the lines, in the order they were written");
        ChatLineDTO first = both.lines().get(0);
        expectTrue(first.timeMillis() >= before && first.timeMillis() <= System.currentTimeMillis(),
                "a line carries the time it was written");

        // Only what is new travels.
        expect("[Bella: Hi Avi]", describe(engine.getChatLines(1)), "a caller at version 1 gets the second line only");
        ChatLinesDTO nothingNew = engine.getChatLines(2);
        expect(0, nothingNew.lines().size(), "a caller that is up to date gets no lines");
        expect(2, nothingNew.version(), "a caller that is up to date keeps its version");
        engine.sendChatLine("Avi", "Third");
        expect("[Avi: Third]", describe(engine.getChatLines(2)), "the next line reaches a caller at version 2");

        // A version the chat never had gets the whole chat, like a client that outlived the server.
        expect(3, engine.getChatLines(99).lines().size(), "a version from the future gets everything");
        expect(3, engine.getChatLines(-1).lines().size(), "a negative version gets everything");

        // What is refused changes nothing.
        expectThrows(InvalidChatLineException.class, () -> engine.sendChatLine("Avi", "   "), "an empty line");
        expectThrows(InvalidChatLineException.class, () -> engine.sendChatLine("Avi", null), "no line at all");
        expectThrows(InvalidChatLineException.class, () -> engine.sendChatLine("Avi", "שלום"),
                "a line that is not in English");
        expectThrows(UserNotFoundException.class, () -> engine.sendChatLine("Nobody", "Hello"), "an unknown user");
        expect(3, engine.getChatLines(0).version(), "the refused lines were not added");
    }

    static String describe(ChatLinesDTO chat) {
        return chat.lines().stream().map(line -> line.userName() + ": " + line.text()).toList().toString();
    }
}
