package entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Object for updating the currently online users, created and sent to all connected clients through ObjectOutputStream
 * when any implementation of the UserConnectionCallback has been invoked.
 * E.g. if onUserConnectListener(User) fired then the handledUser is the user which connected
 * and triggered the UserConnectionCallback.
 */
public class UserSet implements Serializable {
    private HashSet<User> setOfUsers;
    private User handledUser;

    public HandledUserType getUserType() {
        return userType;
    }

    private HandledUserType userType;

    /**
     * Constructor
     * @param set set of users in hashset, won't add duplicate users.
     * @param handledUser the user of onUserConnectListener(User) / onUserDisconnectListener(User)
     */
    public UserSet(HashSet<User> set, User handledUser, HandledUserType userType) {
        this.setOfUsers =  set;

        // connect / disconnect
        this.userType = userType;
        // the user whose connection status triggered the UserConnectionCallBack
        this.handledUser = handledUser;

    }

    /**
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
