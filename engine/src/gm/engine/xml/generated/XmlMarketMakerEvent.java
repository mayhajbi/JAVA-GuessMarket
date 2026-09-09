package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * A single {@code <event id="..."/>} reference inside a {@code <GM-market-maker>} block.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMarketMakerEvent {

    @XmlAttribute(name = "id")
    private Integer id;

    public Integer getId() {
        return id;
    }
}
