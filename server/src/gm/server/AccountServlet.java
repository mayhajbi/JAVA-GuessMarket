package gm.server;

import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The account of the user of the session, and only of that user - a name sent by the client is never read:
 * the details ({@code /account}) and the movements of the money ({@code /account/log}).
 */
public class AccountServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        Object account = "/account/log".equals(request.getServletPath())
                ? engine.getAccountEntries(username)
                : engine.getUserDetails(username);
        ServletUtils.writeJson(response, account);
    }
}
