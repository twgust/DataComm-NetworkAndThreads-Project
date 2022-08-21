package server.controller.threads;


import entity.ConnectionEventType;
import entity.User;
import entity.UserSet;
import server.serverinterface.UserSetProducedEvent;
import server.controller.buffer.UserBuffer;
import server.controller.ServerLogger;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * @author twgust
 * Typically invoked after a user connection or disconnect.
 * The Interface implementation of UserConnectionEvent starts this producer by
 * invoking updateUserSet(User,ConnectionEventType);
 * Given that information a new UserSet (See entity package) is produced.
 * Finally, once a UserSet has been produced, the Controller is notified through the UserSetProducedEvent
 */
public class UserSetProducer{
    private final ServerLogger logger;

    private final UserSetProducedEvent userSetProducedEvent;
    private final UserBuffer userBuffer;

    private ExecutorService service;


    public UserSetProducer(ServerLogger logger, UserBuffer userBuffer, UserSetProducedEvent event){
        this.logger = logger;
        this.userBuffer = userBuffer;
        this.userSetProducedEvent = event;
    }
    public void setSingleThreadExecutor(ThreadPoolExecutor singleThreadExecutor){
        this.service = singleThreadExecutor;
    }
    public synchronized void updateUserSet(User user, ConnectionEventType type){
        String thread = Thread.currentThread().getName();

        String logUpdateUserSetMsg = "Executing -> [TASK: Produce-OnlineList] >> Running";
        logger.logEvent(Level.INFO,thread,logUpdateUserSetMsg, LocalTime.now());
        switch (type){
            case Connected -> service.execute(()->{
                HashSet<User> userHashSet = userBuffer.getHashSet();
                UserSet set = new UserSet(userHashSet, user,  ConnectionEventType.Connected);
                userSetProducedEvent.userSetProduced(set);
            });
            case Disconnected -> service.execute(()->{
                HashSet<User> userHashSet = userBuffer.getHashSet();
                UserSet set = new UserSet(userHashSet, user,  ConnectionEventType.Disconnected);
                userSetProducedEvent.userSetProduced(set);
            });
        }
    }
}
