package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * A single {@code <GM-event>} element.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlEvent {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private Integer id;

    @XmlElement(name = "description")
    private String description;

    /**
     * The commission element. The schema of the exercise spells it "comision", while the description
     * table spells it "commission" - both spellings are accepted so that a data file cannot fail on
     * this difference.
     */
    @XmlElement(name = "comision")
    private XmlCommission comision;

    @XmlElement(name = "commission")
    private XmlCommission commission;

    @XmlElement(name = "GM-options")
    private XmlOptions options;

    @XmlElement(name = "GM-method")
    private XmlMethod method;

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public XmlCommission getCommission() {
        return comision != null ? comision : commission;
    }

    public XmlOptions getOptions() {
        return options;
    }

    public XmlMethod getMethod() {
        return method;
    }
}
