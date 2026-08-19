package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code <GM-options>} element: the possible outcomes of an event.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlOptions {

    @XmlElement(name = "GM-option")
    private List<String> optionList;

    public List<String> getOptionList() {
        return optionList == null ? new ArrayList<>() : optionList;
    }
}
