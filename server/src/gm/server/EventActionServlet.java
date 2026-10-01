package gm.server;

import gm.dto.CommissionType;
import gm.dto.EventType;
import gm.dto.NewEventRequestDTO;
import gm.dto.OrderRequestDTO;
import gm.dto.OrderSide;
import gm.engine.api.GuessMarketEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The actions of the user of the session on an event, all of them POST: opening it
 * ({@code /event/open?id=}), closing it with its winning option ({@code /event/close?id=&winner=}),
 * buying shares ({@code /event/buy}), placing an order ({@code /event/order}) and creating a new
 * event ({@code /event/create}); the last three get their details as a JSON body. The user is always the one of the session, never a name sent by
 * the client.
 */
public class EventActionServlet extends GmServlet {

    // object types on purpose: readJson refuses a body where one of them was not sent
    private record BuyRequest(Integer eventId, Integer optionIndex, Long quantity) {
    }

    // a new event: the details every event has, then the ones of the chosen trading method only
    private record CreateBody(String name, String description, Integer commissionPercent,
                              CommissionType commissionType, String firstOption, String secondOption,
                              EventType type) {
    }

    private record LmsrBody(Integer liquidity) {
    }

    private record OrderBookBody(Integer baseValue, Boolean allowMint, Integer initialInvestment) {
    }

    private record OrderBody(Integer eventId, OrderSide side, Integer optionIndex, Long quantity, Double price) {
    }

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = requireUsername(request);
        requirePost(request);
        GuessMarketEngine engine = ServletUtils.getEngine(getServletContext());
        Object result;
        switch (request.getServletPath()) {
            case "/event/open" -> result = engine.openEvent(requireInt(request, "id"), username);
            case "/event/close" -> result = engine.closeEvent(requireInt(request, "id"), username, requireInt(request, "winner"));
            case "/event/buy" -> {
                BuyRequest buy = ServletUtils.readJson(request, BuyRequest.class);
                result = engine.buyShares(buy.eventId(), username, buy.optionIndex(), buy.quantity());
            }
            case "/event/create" -> result = engine.createEvent(readNewEvent(request, username));
            default -> {
                OrderBody order = ServletUtils.readJson(request, OrderBody.class);
                result = engine.submitOrder(new OrderRequestDTO(order.eventId(), username, order.side(),
                        order.optionIndex(), order.quantity(), order.price()));
            }
        }
        ServletUtils.writeJson(response, result);
    }

    private static NewEventRequestDTO readNewEvent(HttpServletRequest request, String username) throws IOException {
        String json = ServletUtils.readBody(request);
        CreateBody event = ServletUtils.parseJson(json, CreateBody.class);
        int liquidity = 0;
        int baseValue = 0;
        boolean allowMint = false;
        int initialInvestment = 0;
        if (event.type() == EventType.LMSR) {
            liquidity = ServletUtils.parseJson(json, LmsrBody.class).liquidity();
        } else {
            OrderBookBody orderBook = ServletUtils.parseJson(json, OrderBookBody.class);
            baseValue = orderBook.baseValue();
            allowMint = orderBook.allowMint();
            initialInvestment = orderBook.initialInvestment();
        }
        return new NewEventRequestDTO(username, event.name(), event.description(), event.commissionPercent(),
                event.commissionType(), event.firstOption(), event.secondOption(), event.type(), liquidity,
                baseValue, allowMint, initialInvestment);
    }
}
