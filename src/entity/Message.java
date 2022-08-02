package entity;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * MESSAGE FROM CLIENT:
 * <p>
 * A Message should consist of one of the following components:
 * Text OR [x]
 * Image OR [x]
 * Text AND Image [x]
 * <p>
 * Image file format can be one of the following:
 * .JPG OR
 * .PNG
 * <p>
 * Relations:
 * Message HAS A User (author of message) [x]
 * Message HAS A List of Users (recipients of message) [x]
 * <p>
 * <p>
 * Extra:
 * TimeDate when Message was received by server (this is handled by server)
 * TimeDate when Message was received by client (see above)
 */
public class Message implements Serializable {
    private String textMessage;
    private ImageIcon image;
    private final User author;
    private final ArrayList<User> recipientList;
    private final MessageType type;

    public Message(String textMessage, ImageIcon imageIcon, User author, ArrayList<User> recipients, MessageType type) {
        this.textMessage = textMessage;
        this.image = imageIcon;
        this.author = author;
        this.recipientList = recipients;
        this.type = type;
    }

    /**
     *
     * @param textMessage
     * @param author
     * @param recipients
     * @param type
     */
    public Message(String textMessage, User author, ArrayList<User> recipients, MessageType type) {
        this.textMessage = textMessage;
        this.author = author;
        this.recipientList = recipients;
        this.type = type;
    }

    /**
     *
     * @param imageIcon
     * @param author
     * @param recipients
     * @param type
     */
    public Message(ImageIcon imageIcon, User author, ArrayList<User> recipients, MessageType type) {
        this.image = imageIcon;
        this.author = author;
        this.recipientList = recipients;
        this.type = type;
    }

    /**
     *
     * @return
     */
    public ImageIcon getImage() {
        if (this.image != null) {
            return this.image;
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String getTextMessage() {
        if (this.textMessage != null) {
            return textMessage;
        }
        return "bad";
    }

    /**
     *
     * @return
     */
    public User getAuthor() {
        return author;
    }

    /**
     *
     * @return
     */
    public ArrayList<User> getRecipientList() {
        return recipientList;
    }

    /**
     *
     * @return
     */
    public MessageType getType() {
        return type;
    }
}
