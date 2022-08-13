package entity;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
/**
 * @author twgust
 */
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
public class Message extends Sendables  {
    private String textMessage;
    private byte[] image;
    private final User author;
    private final ArrayList<User> recipientList;
    private final MessageType type;

    public Message(String textMessage, byte[] image, User author, ArrayList<User> recipients, MessageType type) {
        this.textMessage = textMessage;
        this.image = image;
        this.author = author;
        this.recipientList = recipients;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "textMessage='" + textMessage + '\'' +
                ", author=" + author +
                ", recipientList=" + recipientList +
                ", type=" + type +
                '}';
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
     * @param image
     * @param author
     * @param recipients
     * @param type
     */
    public Message(byte[] image, User author, ArrayList<User> recipients, MessageType type) {
        this.image = image;
        this.author = author;
        this.recipientList = recipients;
        this.type = type;
    }

    /**
     *
     * @return
     */
    public byte[] getImage() {
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

    @Override
    public SendableType getSendableType() {
        return SendableType.Message;
    }
}
