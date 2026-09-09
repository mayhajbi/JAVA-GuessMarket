package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * The {@code <GM-order-book>} element: the parameters of an order book event.
 * <p>
 * {@code allow-mint} is kept as raw text on purpose - the loader checks that it is exactly
 * {@code true} or {@code false} and reports a detailed message if it is not.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlOrderBook {

    @XmlAttribute(name = "allow-mint")
    private String allowMint;

    @XmlAttribute(name = "initial")
    private Integer initial;

    @XmlAttribute(name = "d")
    private Integer d;

    public String getAllowMint() {
        return allowMint;
    }

    public Integer getInitial() {
        return initial;
    }

    public Integer getD() {
        return d;
    }
}
