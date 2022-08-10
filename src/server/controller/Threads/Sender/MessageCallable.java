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

public class MessageCallable implements Callable<ObjectOutputStream> {
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
    public ObjectOutputStream call() throws Exception {
        MessageType type;
        ObjectOutputStream oos;
        oos = client.getOos();
        switch (message.getType()){
            case TEXT -> {
                String threadName = Thread.currentThread().getName();
                try {
                    String logSendObject = "sending: " + message.toString() + " -->" + client.getSocket().getRemoteSocketAddress().toString();
                    //logger.logEvent(Level.INFO, logSendObject, LocalTime.now());

                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                    System.out.print("MESSAGESENDER END " + Thread.currentThread().getName());
                    return oos;

                } catch (IOException e) {
                    String logSendObjectException = "failed to send: " + message.toString() + " --> " + client.getSocket().getRemoteSocketAddress().toString();
                    //logger.logEvent(Level.WARNING, logSendObjectException, LocalTime.now() );
                    e.printStackTrace();
                }
            }
            case IMAGE -> {
                byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                assert message.getAuthor().getAvatarAsByteBuffer() != null;
                try{
                    System.out.println(byteBuffer);
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

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            case TEXT_IMAGE -> {
                System.out.println("");
                byte[] byteBuffer = message.getAuthor().getAvatarAsByteBuffer();
                assert message.getAuthor().getAvatarAsByteBuffer() != null;
                try{

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

                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
