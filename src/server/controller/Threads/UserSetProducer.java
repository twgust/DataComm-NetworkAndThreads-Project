package server.controller.Threads;


import entity.ConnectionEventType;
import entity.User;
import entity.UserSet;
import server.ServerInterface.MessageReceivedEvent;
import server.ServerInterface.UserSetProducedEvent;
import server.controller.Buffer.UserBuffer;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

public class UserSetProducer{
    private final UserBuffer userBuffer;
    private final ExecutorService service;
    private final ProducerRunnable producer;

    private User user;
    private Queue<User> queue;

    private UserSetProducedEvent userSetProducedEvent;

    public UserSetProducer(UserBuffer userBuffer, UserSetProducedEvent event){
        this.userBuffer = userBuffer;
        this.producer = new ProducerRunnable();
        this.userSetProducedEvent = event;
        service = Executors.newSingleThreadExecutor();


    }
    public void addListener(UserSetProducedEvent listener){
        this.userSetProducedEvent = listener;
    }
    public synchronized void queueUserSetProduction(User user, ConnectionEventType type){
        queue.add(user);
        notify();
    }
    public synchronized User pollQueue() throws InterruptedException {
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
                    User user  = pollQueue();
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
