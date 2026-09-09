package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code <GM-market-maker>} element: the events a single user is the market maker of.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMarketMaker {

    @XmlElement(name = "event")
    private List<XmlMarketMakerEvent> eventList;

    public List<XmlMarketMakerEvent> getEventList() {
        return eventList == null ? new ArrayList<>() : eventList;
    }
}
