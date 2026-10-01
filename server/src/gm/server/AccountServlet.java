package gm.server;

import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The account of the user of the session, and only of that user - a name sent by the client is never read:
 * the details ({@code /account}), the movements of the money ({@code /account/log}), the balance over time
 * ({@code /account/history}) and adding money
 * ({@code /account/deposit}, POST).
 */
public class AccountServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        Object account;
        switch (request.getServletPath()) {
            case "/account/log" -> account = engine.getAccountEntries(username);
            case "/account/history" -> account = engine.getUserBalanceHistory(username);
            case "/account/deposit" -> {
                requirePost(request);
                account = engine.deposit(username, requireDouble(request, "amount"));
            }
            default -> account = engine.getUserDetails(username);
        }
        ServletUtils.writeJson(response, account);
    }
}
