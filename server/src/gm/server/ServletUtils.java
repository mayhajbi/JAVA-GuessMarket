package gm.server;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import gm.engine.api.GuessMarketEngine;
import gm.engine.impl.GuessMarketEngineImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.util.stream.Collectors;

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
     * Reads the JSON body of the request as a record of the given type. Every field of the record is
     * required: Gson leaves a field that was not sent (or has a value that does not fit) as null, so a
     * record of this kind has to use object types (Integer, Long, Double), never primitives.
     *
     * @throws GmServlet.BadRequestException with the reason when the body is missing, is not valid JSON,
     *                                       or one of the fields is missing
     */
    public static <T extends Record> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        return parseJson(readBody(request), type);
    }

    /**
     * The text of the body of the request, for a request that is read as more than one record.
     */
    public static String readBody(HttpServletRequest request) throws IOException {
        return request.getReader().lines().collect(Collectors.joining("\n"));
    }

    /**
     * Like {@link #readJson}, out of the text of the body.
     */
    public static <T extends Record> T parseJson(String json, Class<T> type) {
        T body;
        try {
            body = GSON.fromJson(json, type);
        } catch (JsonParseException e) {
            throw new GmServlet.BadRequestException("The body is not a valid JSON: " + e.getMessage());
        }
        if (body == null) {
            throw new GmServlet.BadRequestException("The request has no body. Send the details as JSON.");
        }
        try {
            for (RecordComponent field : type.getRecordComponents()) {
                if (field.getAccessor().invoke(body) == null) {
                    throw new GmServlet.BadRequestException(
                            "The field '" + field.getName() + "' is missing or has a wrong value.");
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return body;
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
