package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The {@code <GM-LMSR>} element: the liquidity value (b) of the event.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLmsr {

    @XmlElement(name = "b")
    private Integer b;

    public Integer getB() {
        return b;
    }
}
