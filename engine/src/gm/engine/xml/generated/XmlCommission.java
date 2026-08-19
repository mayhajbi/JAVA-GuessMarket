package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * The commission element: a percentage as its text, and its collecting method as an attribute.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlCommission {

    @XmlValue
    private Integer value;

    @XmlAttribute(name = "type")
    private String type;

    public Integer getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
