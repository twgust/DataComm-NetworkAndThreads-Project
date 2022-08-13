package server.controller.Threads.Sender;

import entity.Message;
import entity.MessageType;
import server.Entity.Client;
import server.RunServer;
import server.ServerInterface.UserConnectionEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

public class MessageCallable implements Callable<Client> {
    private final Message message;
    private Client client;
    private final UserConnectionEvent userConnectionEvent;
    public MessageCallable(Message message, Client client, UserConnectionEvent event) {
        this.message = message;
        this.client = client;
        this.userConnectionEvent = event;
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
            switch (message.getType()) {
                case TEXT -> {
                    String threadName = Thread.currentThread().getName();
                    try{

                        oos.writeObject(message);
                        oos.flush();
                        oos.reset();
                    }catch (IOException e){
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
                        userConnectionEvent.onUserDisconnectListener(client.getUser());
                        e.printStackTrace();
                    }
                    return client;
                }
            }
        return client;
    }

    }


