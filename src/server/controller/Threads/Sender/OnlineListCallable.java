package server.controller.Threads.Sender;

import entity.Sendables;
import server.Entity.Client;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;

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
    public Client call() throws Exception {
        String thread = Thread.currentThread().getName();

        try {
            ObjectOutputStream oos;
            oos = client.getOos();
            oos.writeObject(set);
            oos.flush();
            logger.logEvent(Level.INFO, " updated online lists for " + client.getUser().getUsername(), LocalTime.now());
            oos.reset();
            return client;
        } catch (IOException e) {
            String logClientUpdateException = thread + " " + " ";
            logger.logEvent(Level.WARNING, logClientUpdateException, LocalTime.now());
            e.printStackTrace();
            return null;
        }

    }
}

