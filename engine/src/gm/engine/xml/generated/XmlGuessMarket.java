package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The root element of the data file: {@code <Guess-Market>}.
 * <p>
 * The classes of this package describe the structure of the XML file only. They are filled by JAXB
 * and are never used outside the loading process - the loader converts them into the core objects of
 * the engine.
 */
@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlGuessMarket {

    @XmlElement(name = "GM-events")
    private XmlEvents events;

    @XmlElement(name = "GM-users")
    private XmlUsers users;

    public XmlEvents getEvents() {
        return events;
    }

    public XmlUsers getUsers() {
        return users;
    }
}
