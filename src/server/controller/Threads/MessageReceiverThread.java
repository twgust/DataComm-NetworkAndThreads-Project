package server.controller.Threads;

import entity.Message;
import server.Entity.Client;
import server.ServerInterface.MessageReceivedEvent;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalTime;
import java.util.logging.Level;

/**
 * One thread per client connection, continuously listens to incoming messages
 * Threads are never freed unless clients disconnect
 */
public class MessageReceiverThread implements Runnable {
    private MessageReceivedEvent listener;
    private final ServerLogger logger;
    private final String ip;
    private final ObjectInputStream ois;

    /**
     * @param client Client which is assigned the thread
     * @param logger logger for gui and .txt file
     */
    public MessageReceiverThread(Client client, ServerLogger logger ) {
        this.logger = logger;
        ip = client.getSocket().getRemoteSocketAddress().toString();
        ois = client.getOis();
    }
    public void addListener(MessageReceivedEvent listener){
        this.listener = listener;
    }
    @Override
    public void run() {
        while (true) {
            System.out.println("ok");
            try {
                Object o = ois.readObject();
                if (o instanceof Message) {
                    Message message = (Message) o;
                    System.out.println(message);
                    String logMessageReceived = Thread.currentThread().getName() + "received from client @ " + ip
                            + "\n" + message;
                    logger.logEvent(Level.INFO, logMessageReceived, LocalTime.now());
                    listener.onMessageReceivedEvent(message);
                }
            } catch ( ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }

        }
    }
}
