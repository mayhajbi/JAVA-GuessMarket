package gm.server;

import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The chat of the users: the lines a client does not have yet ({@code /chat?chatversion=}, with the version
 * of the chat the client already has) and adding a line ({@code /chat/send}, POST, with the text as a
 * JSON body). The line is always written by the user of the session.
 */
public class ChatServlet extends GmServlet {

    // an object type on purpose: readJson refuses a body where the text was not sent
    private record ChatBody(String text) {
    }

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        if ("/chat/send".equals(request.getServletPath())) {
            requirePost(request);
            engine.sendChatLine(username, ServletUtils.readJson(request, ChatBody.class).text());
            return;
        }
        ServletUtils.writeJson(response, engine.getChatLines(requireInt(request, "chatversion")));
    }
}
