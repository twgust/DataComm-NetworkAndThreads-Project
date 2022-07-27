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
    private ImageIcon image;
    public User(String username, ImageIcon image){
        this.username = username;
        this.image = image;
    }
    public User(String userName){
        this.username = userName;
    }

    public String getUsername() {
        return username;
    }

    public ImageIcon getImage() {
        return image;
    }

    public String printUser(){
        StringBuilder buildUserPrintMsg = new StringBuilder();
        if(getUsername() != null){
            buildUserPrintMsg.append("USERNAME: ").append(getUsername());
            if(getImage() != null){
                buildUserPrintMsg.append("Image: ").append("True");
                return buildUserPrintMsg.toString();
            }
            else buildUserPrintMsg.append("Image: ").append("False");
        }
        return buildUserPrintMsg.toString();
    }
}
