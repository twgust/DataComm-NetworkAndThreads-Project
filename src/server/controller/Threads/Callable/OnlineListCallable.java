package server.controller.Threads.Callable;

import entity.Sendables;
import server.Entity.Client;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 *
 * Callable - asynchronous 'update online lists' task invoked from ObjectSenderThread on user connect/disconnect
 * @author twgust
 */
public class OnlineListCallable implements Callable<Client> {

    private final ServerLogger logger;
    private final Sendables set;
    private final Client client;

    /**
     *
     * @param logger ServerLogger
     * @param set Set of currently online users
     * @param client the client which is going to receive the updated list
     */
    public OnlineListCallable(ServerLogger logger, Sendables set, Client client) {
        this.logger = logger;
        this.set = set;
        this.client = client;
    }

    @Override
    public Client call() {
        String thread = Thread.currentThread().getName();
        String ip = client.getSocket().getLocalAddress().toString() + ":" + client.getSocket().getLocalPort();
        logger.logEvent(Level.INFO, thread, "Executing ->[ TASK: Update-OnlineLists " + client.getUser() + "]", LocalTime.now());

        try {
            ObjectOutputStream oos;
            oos = client.getOos();
            oos.writeObject(set);
            oos.flush();
            oos.reset();

            logger.logEvent(Level.INFO, thread, "[TASK: Update-OnlineLists, " + client.getUser() + "] >> Completed", LocalTime.now());
            return client;

        } catch (IOException e) {
            String logClientUpdateException = "IOException encountered in ---[TASK: UPDATE-OnlineLists]";
            logger.logEvent(Level.WARNING, thread, logClientUpdateException, LocalTime.now());
            e.printStackTrace();
            return null;
        }
    }
}

