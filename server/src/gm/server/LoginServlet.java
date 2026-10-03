package gm.server;

import gm.engine.exception.UserInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Logs a user in by name: 200 and the name is saved in the session; a name that is already
 * taken gives 401, and a missing name gives 409, as in the course example.
 */
public class LoginServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requirePost(request);
        if (ServletUtils.getUsername(request) != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        try {
            String name = ServletUtils.getEngine(getServletContext()).registerUser(request.getParameter(ServletUtils.USERNAME)).name();
            request.getSession(true).setAttribute(ServletUtils.USERNAME, name);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (UserInputException e) {
            int status = e.getKind() == UserInputException.Kind.UNAVAILABLE_NAME
                    ? HttpServletResponse.SC_UNAUTHORIZED : HttpServletResponse.SC_CONFLICT;
            fail(response, status, e.getTitle(), e.getMessage());
        }
    }
}
