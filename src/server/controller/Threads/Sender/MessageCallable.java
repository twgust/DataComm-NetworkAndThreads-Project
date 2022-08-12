package server.controller.Threads.Sender;

import entity.Message;
import entity.MessageType;
import server.Entity.Client;
import server.RunServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;

public class MessageCallable implements Callable<Client> {
    private final Message message;
    private Client client;

    public MessageCallable(Message message, Client client) {
        this.message = message;
        this.client = client;
    }

    /**
     * @return returns ObjectOutputStream in a not busy state
     * @throws Exception if it throws exception close the socket
     */
    @Override
    public Client call() throws IOException {
        MessageType type;
        ObjectOutputStream oos;
        oos = client.getOos();
        switch (message.getType()){
            case TEXT -> {
                String threadName = Thread.currentThread().getName();
                oos.writeObject(message);
                oos.flush();
                oos.reset();
                return client;
            }

            case IMAGE -> {
                byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                assert message.getAuthor().getAvatarAsByteBuffer() != null;

                    System.out.println(byteBuffer);
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


            }

            case TEXT_IMAGE -> {
                System.out.println("");
                byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                assert message.getAuthor().getAvatarAsByteBuffer() != null;
                    ByteArrayInputStream bais = new ByteArrayInputStream(byteBuffer);
                    BufferedImage img = ImageIO.read(bais);
                    String folder = RunServer.getProgramPath2();
                    String fileSeparator = System.getProperty("file.separator");
                    String newDir = folder + fileSeparator + "User Avatars" + fileSeparator;
                    ImageIO.write(img, "jpg", new File(newDir));
                    bais.close();

                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                }
            }
        return null;
    }
    }

