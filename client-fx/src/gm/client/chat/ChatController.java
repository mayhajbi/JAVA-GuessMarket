package gm.client.chat;

import gm.dto.ChatLineDTO;
import gm.dto.ChatLinesDTO;
import gm.engine.api.GuessMarketEngine;
import gm.ui.fx.common.Dialogs;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import java.util.Locale;

/**
 * The chat screen (bonus): everything the users wrote, and a line to add to it. The chat lives on the
 * server, so every user sees what every other user writes. Only the lines that are new are pulled, the
 * way the chat of the course example does: the screen remembers the version of the chat it shows.
 */
public class ChatController {

    /** The time the line was written, who wrote it and what it says. */
    private static final String LINE_FORMAT = "%tH:%tM:%tS | %s: %s%n";

    @FXML private ToggleButton autoScrollButton;
    @FXML private TextArea chatLinesArea;
    @FXML private TextField lineField;

    private GuessMarketEngine engine;
    private String userName;
    /** Volatile: the automatic updates read it on their own thread, to ask only for what is new. */
    private volatile int version;

    public void start(GuessMarketEngine engine, String userName) {
        this.engine = engine;
        this.userName = userName;
    }

    /**
     * @return the version of the chat this screen shows
     */
    public int version() {
        return version;
    }

    /**
     * Pulls the new lines from the engine right now.
     */
    public void refresh() {
        showChatLines(engine.getChatLines(version));
    }

    /**
     * Adds the lines that are new to the screen. With auto scroll the screen follows the last line;
     * without it the screen stays where the user is reading.
     */
    public void showChatLines(ChatLinesDTO chat) {
        if (chat.version() == version) {
            return;
        }
        version = chat.version();
        StringBuilder text = new StringBuilder();
        for (ChatLineDTO line : chat.lines()) {
            long time = line.timeMillis();
            text.append(String.format(Locale.ROOT, LINE_FORMAT, time, time, time, line.userName(), line.text()));
        }
        if (autoScrollButton.isSelected()) {
            chatLinesArea.appendText(text.toString());
            chatLinesArea.positionCaret(chatLinesArea.getLength());
        } else {
            int caretPosition = chatLinesArea.getCaretPosition();
            double scrollTop = chatLinesArea.getScrollTop();
            chatLinesArea.appendText(text.toString());
            chatLinesArea.positionCaret(caretPosition);
            chatLinesArea.setScrollTop(scrollTop);
        }
    }

    @FXML
    private void onSend() {
        try {
            engine.sendChatLine(userName, lineField.getText());
        } catch (RuntimeException refused) {
            Dialogs.showError("The line was not sent", refused);
            return;
        }
        lineField.clear();
        refresh();
    }
}
