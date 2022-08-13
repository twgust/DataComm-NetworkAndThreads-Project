package server.controller.Threads;


import entity.ConnectionEventType;
import entity.User;
import entity.UserSet;
import server.ServerInterface.UserSetProducedEvent;
import server.controller.Buffer.UserBuffer;
import server.controller.ServerLogger;

import java.util.HashSet;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;
/**
 * @author twgust
 */
public class UserSetProducer{
    private final ServerLogger logger;

    private final UserSetProducedEvent userSetProducedEvent;
    private final UserBuffer userBuffer;
    private Queue<User> queue;

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
        switch (type){
            case Connected -> service.execute(()->{
                HashSet<User> userHashSet = userBuffer.getHashSet();
                UserSet set = new UserSet(userHashSet, user,  ConnectionEventType.Connected);
                System.out.println("SIZE " + set.getUserSet().size());
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
