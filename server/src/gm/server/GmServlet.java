package gm.server;

import gm.engine.exception.GuessMarketException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.Function;

/**
 * Base class of the servlets: runs every request under the engine lock and turns errors into
 * a status code with a JSON object of the title and the message, in one single place.
 */
public abstract class GmServlet extends HttpServlet {

    /**
     * Thrown by {@link #requireUsername} when the request has no logged in user.
     */
    protected static class NotLoggedInException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        NotLoggedInException() {
            super("You are not logged in. Log in and try again.");
        }
    }

    /**
     * Thrown when a parameter of the request is missing or is not what the request needs.
     */
    protected static class BadRequestException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        BadRequestException(String message) {
            super(message);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            synchronized (ServletUtils.LOCK) {
                handle(request, response);
            }
        } catch (NotLoggedInException e) {
            fail(response, HttpServletResponse.SC_UNAUTHORIZED, "Not logged in", e.getMessage());
        } catch (BadRequestException e) {
            fail(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (GuessMarketException e) {
            fail(response, HttpServletResponse.SC_BAD_REQUEST, e.getTitle(), e.getMessage());
        } catch (RuntimeException e) {
            fail(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error",
                    "The server could not process the request. Try again.");
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

    protected static int requireInt(HttpServletRequest request, String name) {
        return requireNumber(request, name, Integer::parseInt, "a whole number");
    }

    protected static double requireDouble(HttpServletRequest request, String name) {
        return requireNumber(request, name, Double::parseDouble, "a number");
    }

    /**
     * Reads a parameter that has to be a number.
     *
     * @param parser converts the text, and throws a {@link NumberFormatException} when it is not such a number
     * @param kind   what the parameter has to be, for the message
     */
    private static <T> T requireNumber(HttpServletRequest request, String name, Function<String, T> parser,
                                       String kind) {
        String value = request.getParameter(name);
        if (value == null) {
            throw new BadRequestException("The parameter '" + name + "' is missing.");
        }
        try {
            return parser.apply(value.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("The parameter '" + name + "' must be " + kind + ", but it is '" + value + "'.");
        }
    }

    /**
     * An action that changes data is only accepted as POST.
     */
    protected static void requirePost(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            throw new BadRequestException("This request changes data, so it must be sent as POST.");
        }
    }

    /**
     * Answers a refusal with a JSON object that has the title of the error dialog and its message.
     */
    protected static void fail(HttpServletResponse response, int status, String title, String message)
            throws IOException {
        response.setStatus(status);
        ServletUtils.writeJson(response, new ErrorBody(title, message));
    }

    /** What the client reads out of the answer to a refused request. */
    private record ErrorBody(String title, String message) {
    }
}
