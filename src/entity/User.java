package entity;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 *
 * A User should consist of the following components:
 * Username (String) OR
 * Username AND Image
 *
 */
public class User implements Serializable {
    private String username;
    //private ImageIcon image;
    /*
    public User(String username, ImageIcon image){
        this.username = username;
        this.image = image;
    }

     */
    public User(String userName){
        this.username = userName;
    }

    public String getUsername() {
        return username;
    }
    /*
    public ImageIcon getImage() {
        return image;
    }
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
    @Override
    public int hashCode()
    {
        int result = username.hashCode();
        result = 31 * result;
        return result;
    }
    @Override
    public String toString() {
        return "{" + "name='" + username + "'}";
    }
}
