import gm.client.ServerException;
import gm.client.chat.ChatController;
import gm.dto.ChatLineDTO;
import gm.dto.ChatLinesDTO;
import gm.engine.api.GuessMarketEngine;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * The chat screen (bonus): the lines that are new are added once, the screen remembers the version it
 * shows, a line that was sent (with the button or with Enter) leaves the field empty and appears on the
 * screen at once, and a refused line stays in the field. Needs nothing running: the screen works here
 * against a canned engine.
 */
public class ChatViewTest extends Check {

    /** The lines the canned engine holds: what was sent through it. */
    static final List<ChatLineDTO> sent = new ArrayList<>();
    static final String REFUSED = "refuse me";
    static final String REFUSAL = "The chat line is refused.";

    public static void main(String[] args) {
        run("chat-view", ChatViewTest::check);
    }

    static void check() throws Exception {
        FxThread.run(() -> chatScreen());
    }

    static void chatScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(ChatController.class.getResource("chat.fxml"));
        Pane root = loader.load();
        ChatController chat = loader.getController();
        TextArea lines = (TextArea) loader.getNamespace().get("chatLinesArea");
        TextField field = (TextField) loader.getNamespace().get("lineField");
        Label versionLabel = (Label) loader.getNamespace().get("chatVersionLabel");
        chat.start(cannedEngine(), "Dana");
        expect("Chat Version: 0", versionLabel.getText(), "the version label of a new screen");

        expect(0, chat.version(), "a new screen is at version 0");
        chat.showChatLines(new ChatLinesDTO(List.of(), 0));
        expect("", lines.getText(), "an answer without lines shows nothing");

        ChatLinesDTO two = new ChatLinesDTO(List.of(line("Avi", "Hello"), line("Bella", "Hi Avi")), 2);
        chat.showChatLines(two);
        expect(2, chat.version(), "the screen remembers the version it shows");
        expect(2L, lines.getText().lines().count(), "every line of the chat is a line on the screen");
        expectTrue(lines.getText().lines().findFirst().orElse("").endsWith("| Avi: Hello"),
                "a line shows who wrote it and what");

        chat.showChatLines(two);
        expect(2L, lines.getText().lines().count(), "the same answer again adds nothing");

        chat.showChatLines(new ChatLinesDTO(List.of(line("Avi", "Third")), 3));
        expect(3L, lines.getText().lines().count(), "only the new line is added");
        expect(3, chat.version(), "the version follows the chat");
        expect("Chat Version: 3", versionLabel.getText(), "the version label follows the chat");

        // Sending: the canned engine holds what was sent, and answers with it from the asked version on.
        sent.addAll(List.of(line("Avi", "Hello"), line("Bella", "Hi Avi"), line("Avi", "Third")));
        field.setText("My line");
        ((Button) root.lookup(".button")).fire();
        expect("", field.getText(), "the field is empty after a line was sent");
        expect(4, chat.version(), "a line that was sent is pulled at once");
        expectTrue(lines.getText().strip().endsWith("| Dana: My line"), "a line that was sent appears on the screen");

        sent.add(line("Maximilian-Alexander", "Hi"));
        chat.refresh();
        expectTrue(lines.getText().strip().endsWith("| Maximilian: Hi"), "a long name is cut to 10 characters");

        field.setText("By enter");
        Screens.fire(field);
        expect("", field.getText(), "Enter in the field sends the line");
        expectTrue(lines.getText().strip().endsWith("| Dana: By enter"), "a line that was sent with Enter appears");

        field.setText(REFUSED);
        Screens.fire(field);
        expect(REFUSED, field.getText(), "a line the engine refuses stays in the field");
        expect("[Message not sent: " + REFUSAL + "]", Screens.closeDialogs().toString(),
                "a refused line is reported with its reason");
    }

    static ChatLineDTO line(String userName, String text) {
        return new ChatLineDTO(userName, System.currentTimeMillis(), text);
    }

    static boolean send(String userName, String text) {
        if (REFUSED.equals(text)) {
            throw new ServerException(REFUSAL);
        }
        return sent.add(line(userName, text));
    }

    /** An engine that keeps the lines it is sent (but one it refuses), and answers with the ones after the asked version. */
    static GuessMarketEngine cannedEngine() {
        return (GuessMarketEngine) Proxy.newProxyInstance(ChatViewTest.class.getClassLoader(),
                new Class<?>[] {GuessMarketEngine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "sendChatLine" -> send((String) args[0], (String) args[1]);
                    case "getChatLines" -> new ChatLinesDTO(sent.subList((int) args[0], sent.size()), sent.size());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
