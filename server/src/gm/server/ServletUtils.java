package gm.server;

import com.google.gson.Gson;
import gm.engine.api.GuessMarketEngine;
import gm.engine.impl.GuessMarketEngineImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Shared helpers of the servlets: the single engine of the server and the user name of the
 * current session.
 */
public class ServletUtils {

    public static final String USERNAME = "username";

    private static final String ENGINE_ATTRIBUTE_NAME = "engine";

    private static final Gson GSON = new Gson();

    // ponytail: one global lock, because the engine is not thread safe; a lock per event if throughput ever matters
    public static final Object LOCK = new Object();

    public static GuessMarketEngine getEngine(ServletContext servletContext) {
        synchronized (LOCK) {
            if (servletContext.getAttribute(ENGINE_ATTRIBUTE_NAME) == null) {
                servletContext.setAttribute(ENGINE_ATTRIBUTE_NAME, new GuessMarketEngineImpl());
            }
            return (GuessMarketEngine) servletContext.getAttribute(ENGINE_ATTRIBUTE_NAME);
        }
    }

    /**
     * Answers the request with the given object (a DTO or a list of them) as JSON.
     */
    public static void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(GSON.toJson(body));
    }

    /**
     * The user name saved in the session by the login, or null when the request has no session.
     */
    public static String getUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object attribute = session != null ? session.getAttribute(USERNAME) : null;
        return attribute != null ? attribute.toString() : null;
    }
}
