package gm.server;

import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * What is known about one event, by its id: its LMSR state ({@code /event/market}), its order book state
 * ({@code /event/orderbook}) or the value of its options over time ({@code /event/prices}).
 */
public class EventStateServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireUsername(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        int eventId = requireInt(request, "id");
        Object state = switch (request.getServletPath()) {
            case "/event/market" -> engine.getMarketState(eventId);
            case "/event/orderbook" -> engine.getOrderBookState(eventId);
            default -> engine.getEventPriceHistory(eventId);
        };
        ServletUtils.writeJson(response, state);
    }
}
