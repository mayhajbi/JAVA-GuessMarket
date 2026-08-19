package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code <GM-events>} element: all the events of the system.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlEvents {

    @XmlElement(name = "GM-event")
    private List<XmlEvent> eventList;

    public List<XmlEvent> getEventList() {
        return eventList == null ? new ArrayList<>() : eventList;
    }
}
