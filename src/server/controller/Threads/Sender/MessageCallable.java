package server.controller.Threads.Sender;

import entity.Message;
import entity.MessageType;
import server.Entity.Client;

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

        System.out.println("MESSAGESENDER " + Thread.currentThread().getName());
        String threadName = Thread.currentThread().getName();
        String logSendObject = "sending: " + message.toString() + " -->" + client.getSocket().getRemoteSocketAddress().toString();
        //logger.logEvent(Level.INFO, logSendObject, LocalTime.now());


        switch (message.getType()){
            case TEXT -> {
                try {
                    oos.writeObject(message);
                    oos.flush();
                    oos.reset();
                    System.out.println("MESSAGESENDER END " + Thread.currentThread().getName());
                    return oos;
                } catch (IOException e) {
                    String logSendObjectException = "failed to send: " + message.toString() + " --> " + client.getSocket().getRemoteSocketAddress().toString();
                    //logger.logEvent(Level.WARNING, logSendObjectException, LocalTime.now() );
                    e.printStackTrace();
                }
            }
            case IMAGE -> {
                System.out.println();
                return oos;
            }
            case TEXT_IMAGE -> {
                return oos;
            }
        }
        return null;
    }
}
