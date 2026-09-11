package gm.dto;

/**
 * The connection of a single user to a single event.
 *
 * @param event       general details of the event
 * @param marketMaker whether the user is the market maker of the event
 * @param participant whether the user has traded in the event
 */
public record UserEventDTO(EventInfoDTO event, boolean marketMaker, boolean participant) {
}
