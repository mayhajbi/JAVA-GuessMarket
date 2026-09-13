package gm.engine.core;

import gm.engine.exception.UserBlockedException;

import java.io.Serializable;

/**
 * A user of the system: a name and an independent money {@link Account}.
 * <p>
 * User names are unique. They are compared without case, exactly like every other textual input in
 * the system, but the original spelling is kept for display.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Account account;

    public User(String name, double initialCash) {
        this.name = name;
        this.account = new Account(initialCash);
    }

    public String getName() {
        return name;
    }

    public Account getAccount() {
        return account;
    }

    /**
     * Makes sure the user may start a new action: a blocked user may not.
     *
     * @param action what the user tried to do, for the message (for example "buy shares")
     * @throws UserBlockedException when the balance of the user dropped below zero at some point
     */
    public void requireNotBlocked(String action) {
        if (account.isBlocked()) {
            throw new UserBlockedException(name, action);
        }
    }
}
