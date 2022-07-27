package entity;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * MESSAGE FROM CLIENT:
 *
 * A Message should consist of one of the following components:
 * Text OR [x]
 * Image OR [x]
 * Text AND Image [x]
 *
 * Image file format can be one of the following:
 * .JPG OR
 * .PNG
 *
 * Relations:
 * Message HAS A User (author of message) [x]
 * Message HAS A List of Users (recipients of message) [x]
 *
 *
 * Extra:
 * TimeDate when Message was received by server (this is handled by server)
 * TimeDate when Message was received by client (see above)
 */
public class Message {
    private String textMessage;
    private ImageIcon image;
    private User author;
    private ArrayList<User> recipientList;

    public Message(String textMessage, ImageIcon imageIcon, User author, ArrayList<User> recipients){
        this.textMessage = textMessage;
        this.image = imageIcon;
        this.author = author;
        this.recipientList = recipients;
    }
    public Message(String textMessage, User author, ArrayList<User> recipients){
        this.textMessage = textMessage;
        this.author = author;
        this.recipientList = recipients;
    }
    public Message(ImageIcon imageIcon, User author, ArrayList<User> recipients){
        this.image = imageIcon;
        this.author = author;
        this.recipientList = recipients;
    }
    public ImageIcon getImage(){
        if(this.image != null){
            return this.image;
        }
        return null;
    }
    public String getTextMessage() {
        if(this.textMessage != null){
            return textMessage;
        }
        return "bad";
    }

    public User getAuthor() {
        return author;
    }

    public ArrayList<User> getRecipientList() {
        return recipientList;
    }


}
