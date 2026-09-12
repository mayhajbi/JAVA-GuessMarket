package gm.dto;

import java.util.Set;

/**
 * Which events to return. An event is included when its type, its status and its commission method
 * are all among the selected values, so an empty set selects no event at all.
 *
 * @param types           the selected trading method families
 * @param statuses        the selected life cycle states
 * @param commissionTypes the selected commission methods
 */
public record EventFilterDTO(Set<EventType> types,
                             Set<EventStatus> statuses,
                             Set<CommissionType> commissionTypes) {

    public EventFilterDTO {
        types = Set.copyOf(types);
        statuses = Set.copyOf(statuses);
        commissionTypes = Set.copyOf(commissionTypes);
    }
}
