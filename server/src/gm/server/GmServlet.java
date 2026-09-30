package gm.server;

import gm.engine.exception.GuessMarketException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base class of the servlets: runs every request under the engine lock and turns errors into
 * a status code with a plain text message, in one single place.
 */
public abstract class GmServlet extends HttpServlet {

    /**
     * Thrown by {@link #requireUsername} when the request has no logged in user.
     */
    protected static class NotLoggedInException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        NotLoggedInException() {
            super("You are not logged in. Please log in first.");
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        try {
            synchronized (ServletUtils.LOCK) {
                handle(request, response);
            }
        } catch (NotLoggedInException e) {
            fail(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (GuessMarketException e) {
            fail(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            fail(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The server failed to handle the request.");
        }
    }

    protected abstract void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;

    protected static String requireUsername(HttpServletRequest request) {
        String username = ServletUtils.getUsername(request);
        if (username == null) {
            throw new NotLoggedInException();
        }
        return username;
    }

    protected static void fail(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().print(message);
    }
}
