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

public class UserSetProducer{
    private final UserBuffer userBuffer;
    private final ExecutorService service;
    private final ProducerRunnable producer;

    private Queue<User> queue;
    private final UserSetProducedEvent userSetProducedEvent;
    private final ServerLogger logger;

    public UserSetProducer(ServerLogger logger, UserBuffer userBuffer, UserSetProducedEvent event){
        this.logger = logger;
        this.userBuffer = userBuffer;
        this.producer = new ProducerRunnable();
        this.userSetProducedEvent = event;
        service = Executors.newSingleThreadExecutor();


    }
    public synchronized void updateUserSet(User user, ConnectionEventType type){
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
    public synchronized void addUserToQueue(User user, ConnectionEventType type){
        queue.add(user);
        notify();
    }
    public synchronized User pollUserFromQueue() throws InterruptedException {
        if (queue.isEmpty()){
            wait();
        }
        return queue.poll();
    }
    public void start() {
        queue = new LinkedBlockingQueue<>();
        service.execute(producer);
        System.out.println("Producer Ready");
    }
    public void stop() throws InterruptedException {
        queue = null;
        boolean res = service.awaitTermination(10, TimeUnit.SECONDS);
    }
    private class ProducerRunnable implements Runnable{
        @Override
        public void run() {
            HashSet<User> userHashSet;
            while(queue != null){
                try{
                    User user  = pollUserFromQueue();
                    userBuffer.put(user);
                    userHashSet = userBuffer.getHashSet();
                    UserSet set = new UserSet(userHashSet, user,  ConnectionEventType.Connected);
                    userSetProducedEvent.userSetProduced(set);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
