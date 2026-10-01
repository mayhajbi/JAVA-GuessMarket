package gm.server;

import gm.dto.CommissionType;
import gm.dto.EventFilterDTO;
import gm.dto.EventStatus;
import gm.dto.EventType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The list of events that match a filter: {@code /events?types=&statuses=&commissions=}. Every parameter
 * is a comma separated list of names (an enum set does not travel in JSON, so the filter travels in the
 * query). A parameter that is left out selects everything, and an empty one selects nothing.
 */
public class EventsServlet extends GmServlet {

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireUsername(request);
        EventFilterDTO filter = new EventFilterDTO(
                selection(request, "types", EventType.class),
                selection(request, "statuses", EventStatus.class),
                selection(request, "commissions", CommissionType.class));
        ServletUtils.writeJson(response, ServletUtils.getEngine(getServletContext()).getEvents(filter));
    }

    private static <E extends Enum<E>> Set<E> selection(HttpServletRequest request, String name, Class<E> type) {
        String value = request.getParameter(name);
        if (value == null) {
            return EnumSet.allOf(type);
        }
        Set<E> selected = EnumSet.noneOf(type);
        for (String part : value.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                selected.add(Enum.valueOf(type, part.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("The parameter '" + name + "' has the value '" + part.trim() + "', which is not one of "
                        + Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "))
                        + ".");
            }
        }
        return selected;
    }
}
