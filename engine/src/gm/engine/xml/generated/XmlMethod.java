package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The {@code <GM-method>} element: the trading method of an event. In this exercise the only
 * supported method is LMSR.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMethod {

    @XmlElement(name = "GM-LMSR")
    private XmlLmsr lmsr;

    public XmlLmsr getLmsr() {
        return lmsr;
    }
}
