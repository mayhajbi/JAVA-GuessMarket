package gm.server;

import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * What is known about one event, by its id: its LMSR state ({@code /event/market}), its order book state
 * ({@code /event/orderbook}), the value of its options over time ({@code /event/prices}) or what a purchase of
 * shares would cost now ({@code /event/quote}), which changes nothing.
 */
public class EventStateServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        int eventId = requireInt(request, "id");
        Object state = switch (request.getServletPath()) {
            case "/event/market" -> engine.getMarketState(eventId);
            case "/event/orderbook" -> engine.getOrderBookState(eventId);
            case "/event/quote" -> engine.quoteShares(eventId, username, requireInt(request, "option"),
                    requireLong(request, "quantity"));
            default -> engine.getEventPriceHistory(eventId);
        };
        ServletUtils.writeJson(response, state);
    }
}
