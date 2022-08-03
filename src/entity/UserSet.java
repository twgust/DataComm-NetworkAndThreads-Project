package entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Object for updating the currently online users, created and sent to all connected clients through ObjectOutputStream
 * when any implementation of the UserConnectionCallback has been invoked.
 * includes the User "handled user"
 * E.g. if onUserConnectListener(User) fired then the handledUser is the user which connected
 * and triggered the UserConnectionCallback.
 */
public class UserSet implements Serializable {
    private HashSet<User> setOfUsers;
    private User handledUser;

    /**
     * Constructor
     * @param set set of users in hashset, won't add duplicate users.
     * @param handledUser the user of onUserConnectListener(User) / onUserDisconnectListener(User)
     */
    public UserSet(HashSet<User> set, User handledUser) {
        this.setOfUsers =  set;
        this.handledUser = handledUser;
    }

    /**
     *
     * @return returns the hashset <User>
     */
    public Set<User> getUserSet()   {
        if(setOfUsers != null){
            return setOfUsers;
        }
        else return null;
    }

    public  User getHandledUser() {
        return handledUser;
    }
}
