package gm.engine.xml.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code <GM-users>} element: all the users of the system.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlUsers {

    @XmlElement(name = "GM-user")
    private List<XmlUser> userList;

    public List<XmlUser> getUserList() {
        return userList == null ? new ArrayList<>() : userList;
    }
}
