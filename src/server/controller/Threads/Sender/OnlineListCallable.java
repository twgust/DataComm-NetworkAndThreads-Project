package server.controller.Threads.Sender;

import entity.Sendables;
import server.Entity.Client;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * @author twgust
 */
public class OnlineListCallable implements Callable<Client> {

    private final ServerLogger logger;
    private final Sendables set;
    private final Client client;

    public OnlineListCallable(ServerLogger logger, Sendables set, Client client) {
        this.logger = logger;
        this.set = set;
        this.client = client;
    }

    @Override
    public Client call() {
        String thread = Thread.currentThread().getName();
        String ip = client.getSocket().getLocalAddress().toString() + ":" + client.getSocket().getLocalPort();
        logger.logEvent(Level.INFO, thread, "Executing ---[ TASK: UPDATE-OnlineLists " + client.getUser() + "]", LocalTime.now());

        try {
            ObjectOutputStream oos;
            oos = client.getOos();
            oos.writeObject(set);
            oos.flush();
            oos.reset();
            logger.logEvent(Level.INFO, thread, "[TASK: UPDATE-OnlineLists, " + client.getUser() + "] >Completed", LocalTime.now());


            return client;
        } catch (IOException e) {
            String logClientUpdateException = "IOException encountered in ---[TASK: UPDATE-OnlineLists]";
            logger.logEvent(Level.WARNING, thread, logClientUpdateException, LocalTime.now());


            e.printStackTrace();
            return null;
        }


    }
}

