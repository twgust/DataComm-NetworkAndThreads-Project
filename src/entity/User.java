package entity;


import javax.swing.*;
import java.io.Serializable;

/**
 *
 * A User should consist of the following components:
 * Username (String) OR
 * Username AND Image
 *
 */
/**
 * @author twgust
 */
public class User extends Sendables {
    private final String username;
    private byte[] avatarBuffer;
    private transient ImageIcon avatar;

    /**
     * TODO, not fully implemented
     * @param userName string
     * @param avatar ImageIcon
     */
    public User(String userName, ImageIcon avatar){
        this.username = userName;
        this.avatar = avatar;
    }

    /**
     * This is the constructor currently in use,
     * ++ ease of use with ObjectInputStream.
     * @param userName username
     * @param img array of bytes representing an image
     */
    public User(String userName, byte[] img){
        this.username = userName;
        this.avatarBuffer = img;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * TODO, not implemented
     * @return ImageIcon (user avatar)
     */
    public ImageIcon getAvatar() {
        return avatar;
    }

    /**
     * @return buffer of bytes representing a jpg.
     */
    public byte[] getAvatarAsByteBuffer() {
        return avatarBuffer;
    }

    /**
     * Overridden equals function:
     *            User user1 = new User("name1","img1"),
     *            User user2 = new User("name1", "img2")
     *            user1.equals(user2) returns true
     * @param o object to compare
     * @return result of comparison True/False
     */
    @Override
    public boolean equals(Object o){
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        // using string.equals,
        // not user.equals which we are overriding.
        if (!username.equals(user.username)) {
            return false;
        }


        return true;
    }

    /**
     * @return hashCode for given user.
     */
    @Override
    public int hashCode()
    {
        int result = username.hashCode();
        result = 31 * result;
        return result;
    }

    /**
     * TODO finalize
     * @return String representation of a User instance
     */
    @Override
    public String toString() {
        return "{" + "name='" + username + "'}";
    }

    @Override
    public SendableType getSendableType() {
        return null;
    }
}
