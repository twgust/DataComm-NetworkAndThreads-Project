package server.Controller.Threads.Callable;

import entity.Message;
import entity.MessageType;
import entity.User;
import server.Entity.Client;
import server.ServerInterface.UserConnectionEvent;
import server.Controller.Buffer.UnsentMessageBuffer;
import server.Controller.ServerLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * @author twgust
 * Callable - asynchronous message sending task invoked from ObjectSenderThread
 */
public class MessageCallable implements Callable<Client> {
    private final ServerLogger logger;

    private final Message message;
    private final Client client;

    private final UserConnectionEvent userConnectionEvent;
    private final UnsentMessageBuffer unsentMessageBuffer;

    /**
     * @author twgust
     * @param logger reference to global logger
     * @param message the message which is going to be sent
     * @param client the receiving client
     * @param event a UserConnectionEvent, should we discover that a client has disconnected we can invoke .onUserDisconnect()
     * @param unsentMessageBuffer if they have disconnected, add the message to the buffer of unsent messages
     */
    public MessageCallable(ServerLogger logger, Message message, Client client, UserConnectionEvent event, UnsentMessageBuffer unsentMessageBuffer) {
        this.logger = logger;
        this.message = message;
        this.client = client;
        this.userConnectionEvent = event;
        this.unsentMessageBuffer = unsentMessageBuffer;
    }

    /**
     * @author twgust
     * @return returns ObjectOutputStream in a not busy state
     * @throws Exception if it throws exception close the socket
     */
    @Override
    public Client call() {
        MessageType type;
        ObjectOutputStream oos;

        oos = client.getOos();
        System.out.println(message.getAuthor() + ": " + message.getTextMessage() + " --> " + client.getUser().getUsername());
        String thread = Thread.currentThread().getName();
        String logFailedMessage = "IOException encountered in ---[TASK Send-Message{" + message.hashCode() + "}]" +
                "\n >> CAUSE: M={hashcode=" + message.hashCode() + "} could not be sent to [" + client.getUser() + "] due to an IOException" +
                "\n >> ACTION: queueing unsendable to message buffer and disconnecting client";
        switch (message.getType()) {
            case TEXT -> {
                try {

                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                    return client;
                } catch (IOException e) {
                    logger.logEvent(Level.WARNING, thread, "{" + logFailedMessage + "\n}\n\n", LocalTime.now());
                    ArrayList<User> recipient = new ArrayList(1);
                    recipient.add(client.getUser());
                    unsentMessageBuffer.put(
                            new Message(message.getTextMessage(), message.getAuthor(), recipient, MessageType.TEXT), client.getUser());
                    userConnectionEvent.onUserDisconnectListener(client.getUser());
                    return null;
                }
            }

            case IMAGE -> {
                byte[] byteBuffer = message.getImage();
                assert message.getImage() != null;

                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
                    BufferedImage img = ImageIO.read(bais);
                    ImageIO.write(img, "jpg", new File("/images/" + message.hashCode() +".jpg"));
                    bais.close();

                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                    return client;

                } catch (IOException e) {
                    logger.logEvent(Level.WARNING, thread, logFailedMessage, LocalTime.now());

                    // creates a new message object, with a recipient list only containing ...
                    // the disconnected user who triggered this catch block
                    ArrayList<User> recipient = new ArrayList<>(1);
                    recipient.add(client.getUser());
                    Message newMessage = new Message(message.getImage(), message.getAuthor(), recipient, MessageType.IMAGE);

                    // put the message to the buffer of messages which failed to send
                    // K= Message, V = User: who dropped connection
                    unsentMessageBuffer.put(newMessage, client.getUser());

                    // fire onUserDisconnectEvent
                    userConnectionEvent.onUserDisconnectListener(client.getUser());
                    e.printStackTrace();
                    return null;
                }
            }

            case TEXT_IMAGE -> {
                try {
                    byte[] byteBuffer = message.getImage();
                    assert message.getImage() != null;

                    ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
                    BufferedImage img = ImageIO.read(bais);
                    ImageIO.write(img, "jpg", new File("images/" + message.hashCode() +".jpg"));

                    bais.close();
                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                    return client;
                } catch (IOException e) {
                    logger.logEvent(Level.WARNING, thread, logFailedMessage, LocalTime.now());

                    // creates a new message object, with a recipient list only containing ...
                    // the disconnected user who triggered this catch block
                    ArrayList<User> recipient = new ArrayList(1);
                    recipient.add(client.getUser());
                    Message newMessage = new Message(message.getTextMessage(), message.getImage(), message.getAuthor(), recipient, MessageType.TEXT_IMAGE);

                    // put the message to the buffer of messages which failed to send
                    // K= Message, V = User: who dropped connection
                    unsentMessageBuffer.put(newMessage, client.getUser());

                    // call onUserDisconnect
                    userConnectionEvent.onUserDisconnectListener(client.getUser());
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

}


