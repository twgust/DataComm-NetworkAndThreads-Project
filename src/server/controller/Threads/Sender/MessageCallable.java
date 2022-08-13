package server.controller.Threads.Sender;

import entity.Message;
import entity.MessageType;
import server.Entity.Client;
import server.RunServer;
import server.ServerInterface.UserConnectionEvent;
import server.controller.Buffer.MessageBuffer;
import server.controller.ServerLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class MessageCallable implements Callable<Client> {
    private final ServerLogger logger;

    private final Message message;
    private final Client client;

    private final UserConnectionEvent userConnectionEvent;
    private final MessageBuffer messageBuffer;
    public MessageCallable(ServerLogger logger, Message message, Client client, UserConnectionEvent event, MessageBuffer messageBuffer) {
        this.logger = logger;
        this.message = message;
        this.client = client;
        this.userConnectionEvent = event;
        this.messageBuffer = messageBuffer;

    }

    /**
     * @return returns ObjectOutputStream in a not busy state
     * @throws Exception if it throws exception close the socket
     */
    @Override
    public Client call()  {
        MessageType type;
        ObjectOutputStream oos;
        oos = client.getOos();
        System.out.println(message.getAuthor() + ": " + message.getTextMessage() + " --> " + client.getUser().getUsername());
        String thread = Thread.currentThread().getName();
        String logFailedMessage = "IOException encountered in ---[TASK Send-Message{"+ message.hashCode() +"}]" +
                "\n>CAUSE: M={hashcode=" + message.hashCode() + "} could not be sent to [" + client.getUser()+"] due to an IOException" +
                "\n>ACTION: queueing unsendable to message buffer and disconnecting client";
        switch (message.getType()) {
                case TEXT -> {
                    try{

                        oos.writeObject(message);
                        oos.flush();
                        oos.reset();
                    }catch (IOException e){
                        logger.logEvent(Level.WARNING, thread, "\n\n{\n" + logFailedMessage + "\n}\n\n", LocalTime.now());
                        messageBuffer.queueMessage(message);
                        userConnectionEvent.onUserDisconnectListener(client.getUser());
                    }
                    return client;
                }

                case IMAGE -> {
                    byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                    assert message.getAuthor().getAvatarAsByteBuffer() != null;

                    try{
                        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
                        BufferedImage img = ImageIO.read(bais);
                        String folder = RunServer.getProgramPath2();
                        String fileSeparator = System.getProperty("file.separator");
                        String newDir = folder + fileSeparator + "messages" + fileSeparator;
                        ImageIO.write(img, "jpg", new File(newDir));
                        bais.close();
                        oos.writeObject(message);
                        oos.flush();
                        oos.reset();

                    }catch (IOException e){


                        logger.logEvent(Level.WARNING, thread, logFailedMessage, LocalTime.now());

                        messageBuffer.queueMessage(message);
                        userConnectionEvent.onUserDisconnectListener(client.getUser());
                        e.printStackTrace();
                    }
                    return client;

                }

                case TEXT_IMAGE -> {
                    try{
                        byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                        assert message.getAuthor().getAvatarAsByteBuffer() != null;
                        ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
                        BufferedImage img = ImageIO.read(bais);
                        String folder = RunServer.getProgramPath2();
                        String fileSeparator = System.getProperty("file.separator");
                        String newDir = folder + fileSeparator + "User Messages" + fileSeparator;
                        ImageIO.write(img, "jpg", new File(newDir));
                        bais.close();

                        oos.writeObject(message);
                        oos.flush();
                        oos.reset();

                    }catch (IOException e){
                        logger.logEvent(Level.WARNING, thread, logFailedMessage, LocalTime.now());
                        messageBuffer.queueMessage(message);
                        userConnectionEvent.onUserDisconnectListener(client.getUser());
                        e.printStackTrace();
                    }
                    return client;
                }
            }
        return client;
    }

    }


