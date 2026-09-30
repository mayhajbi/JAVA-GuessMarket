package gm.server;

import gm.dto.UserSummaryDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The other users of the system, each with what a user may see of another one: the name, the balance and
 * whether it is a market maker. The user of the session is not in the list.
 */
public class UsersServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        ServletUtils.writeJson(response, ServletUtils.getEngine(getServletContext()).getAllUsers().stream()
                .filter(user -> !user.name().equalsIgnoreCase(username))
                .map(user -> new UserSummaryDTO(user.name(), user.balance(), user.marketMaker()))
                .toList());
    }
}
