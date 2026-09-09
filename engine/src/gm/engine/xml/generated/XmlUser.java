package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * A single {@code <GM-user>} element: a name, a starting cash amount, and an optional list of the
 * events this user is the market maker of.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlUser {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "initial-cash")
    private Integer initialCash;

    @XmlElement(name = "GM-market-maker")
    private XmlMarketMaker marketMaker;

    public String getName() {
        return name;
    }

    public Integer getInitialCash() {
        return initialCash;
    }

    public XmlMarketMaker getMarketMaker() {
        return marketMaker;
    }
}
