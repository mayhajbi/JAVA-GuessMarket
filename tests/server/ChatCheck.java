import gm.client.HttpApi;
import gm.client.HttpGuessMarketEngine;
import gm.client.ServerException;
import gm.dto.ChatLinesDTO;
import gm.engine.api.GuessMarketEngine;

import java.util.Map;

/**
 * The chat (bonus) against the running server, with the classes of the client: a client that is not
 * logged in is refused, a line one user sends reaches another user, only the lines that are new travel,
 * and a line that is empty or not in English is refused with the reason.
 */
public class ChatCheck extends Check {

    public static void main(String[] args) {
        run("chat-server", ChatCheck::check);
    }

    static void check() {
        String suffix = String.valueOf(System.nanoTime() % 1_000_000);
        String firstName = "chat_a_" + suffix;
        String secondName = "chat_b_" + suffix;
        HttpApi firstApi = new HttpApi();
        HttpApi secondApi = new HttpApi();
        GuessMarketEngine first = new HttpGuessMarketEngine(firstApi);
        GuessMarketEngine second = new HttpGuessMarketEngine(secondApi);
        try {
            expectTrue(refusal(() -> first.getChatLines(0)).contains("not logged in"),
                    "reading the chat without a session is refused");
            expectTrue(refusal(() -> first.sendChatLine(firstName, "Hello")).contains("not logged in"),
                    "writing to the chat without a session is refused");

            first.registerUser(firstName);
            second.registerUser(secondName);
            // The chat is shared by every user of the server, so the check starts from where it is now.
            int start = second.getChatLines(0).version();

            String text = "Hello from " + firstName;
            first.sendChatLine("someone_else", "  " + text + "  ");
            ChatLinesDTO delta = second.getChatLines(start);
            expect(start + 1, delta.version(), "a line raises the version by one");
            expect(1, delta.lines().size(), "the other user gets the new line only");
            expect(firstName, delta.lines().get(0).userName(), "the writer is the user of the session, not a name that was sent");
            expect(text, delta.lines().get(0).text(), "the text arrives clean");
            expect(0, second.getChatLines(delta.version()).lines().size(), "a client that is up to date gets nothing");

            expectTrue(refusal(() -> first.sendChatLine(firstName, "   ")).contains("message cannot be empty"), "an empty line is refused");
            expectTrue(refusal(() -> first.sendChatLine(firstName, "שלום")).contains("English"),
                    "a line that is not in English is refused");
            expectTrue(refusal(() -> firstApi.get("/chat/send", Map.of())).contains("must be sent as POST"),
                    "sending a line as GET is refused");
            expectTrue(refusal(() -> firstApi.get("/chat", Map.of())).contains("is missing"),
                    "reading the chat without a version is refused");
            expectTrue(refusal(() -> firstApi.post("/chat/send", Map.of(), "{}")).contains("'text' is missing"),
                    "a body without the text is refused");
            expect(delta.version(), second.getChatLines(0).version(), "the refused lines were not added");
        } finally {
            firstApi.shutdown();
            secondApi.shutdown();
        }
    }

    /** The message of the server for a request it refuses; empty when it does not refuse it. */
    static String refusal(Runnable request) {
        try {
            request.run();
        } catch (ServerException refused) {
            return refused.getMessage();
        }
        return "";
    }
}
