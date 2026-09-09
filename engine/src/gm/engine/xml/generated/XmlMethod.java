package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The {@code <GM-method>} element: the trading method of an event, which is a choice between
 * {@code <GM-LMSR>} and {@code <GM-order-book>}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMethod {

    @XmlElement(name = "GM-LMSR")
    private XmlLmsr lmsr;

    @XmlElement(name = "GM-order-book")
    private XmlOrderBook orderBook;

    public XmlLmsr getLmsr() {
        return lmsr;
    }

    public XmlOrderBook getOrderBook() {
        return orderBook;
    }
}
