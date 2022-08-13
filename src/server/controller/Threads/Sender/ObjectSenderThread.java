package server.controller.Threads.Sender;

import entity.*;
import server.Entity.Client;
import server.ServerInterface.UserConnectionEvent;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author twgust
 * Runnable for sending a message to a Client,
 * Writes to connected clients ObjectOutputStream
 */
public class ObjectSenderThread {
    private final ServerLogger logger;
    private final ClientBuffer clientBuffer;
    private final MessageBuffer messageBuffer;
    private final UserBuffer userBuffer;
    private final SendablesBuffer sendablesBuffer;

    private ThreadPoolExecutor threadPoolExecutor;
    private UserConnectionEvent listener;

    /**
     * Constructor,
     * @param logger
     * @param clientBuffer reference to buffer (final)
     * @param userBuffer   reference to buffer (final)
     */
    public ObjectSenderThread(ServerLogger logger, SendablesBuffer buffer,ClientBuffer clientBuffer, UserBuffer userBuffer,
                              MessageBuffer messageBuffer, UserConnectionEvent userConnectionEvent) {
        this.logger = logger;
        this.messageBuffer = messageBuffer;
        this.listener = userConnectionEvent;
        this.clientBuffer = clientBuffer;
        this.userBuffer = userBuffer;
        this.sendablesBuffer = buffer;
    }

    /**
     * Sets executor, invoked by controller
     * @param threadPoolExecutor has to be thread pool
     * @return
     */
    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        assert this.threadPoolExecutor != null;
        return true;
    }
    public synchronized void start()   {
        while (sendablesBuffer != null) {
            try{

                Sendables sendable = sendablesBuffer.dequeueSendable();
                Message message = null;
                UserSet userSet = null;

                switch (sendable.getSendableType()) {
                    case Message -> {
                        message = (Message) sendable;
                        Message finalMessage = message;

                        List<Client> list = new ArrayList<>();
                        message.getRecipientList().forEach(user -> {
                            if(clientBuffer.get(user) == null){
                                System.out.println(user.toString() + " appears to be offline");
                            }
                            else{
                                list.add(clientBuffer.get(user));
                            }

                        });
                        ArrayList<MessageCallable> callables = new ArrayList<>(list.size());
                        System.out.println(list.size());
                        list.forEach(client -> {
                            MessageCallable callable = new MessageCallable(finalMessage, client, listener);
                            callables.add(callable);
                        });



                        List<Future<Client>> resultList = threadPoolExecutor.invokeAll(callables, 2, TimeUnit.SECONDS);
                            for (int i = 0; i < resultList.size(); i++) {
                                    Future<Client> clientel = resultList.get(i);
                                    try{
                                        Client client = clientel.get();

                                    }catch (ExecutionException e){
                                        System.out.println(e.getCause());
                                        e.printStackTrace();
                                    }
                            }
                    }




                    case UserSet -> {
                        System.out.println("USERSET START");
                        userSet = (UserSet) sendable;
                        UserSet finalSet = userSet;
                        ArrayList<OnlineListCallable> callables = new ArrayList<>(clientBuffer.size());
                        System.out.println(callables.size());

                        // set up a collection of all the clients
                        Collection<Client> arr = clientBuffer.allValues();
                        // for each client c,
                        // create a OnlineListCallable(ServerLogger,Sendable, Client c) obj
                        // add the instance to a list of callables.
                        for (Client client: arr) {
                            OnlineListCallable callable = new OnlineListCallable(logger, finalSet, client);
                            callables.add(callable);
                        }
                        // execute all the callables and wait for result, this way we achieve concurrency in message sending
                        // while avoiding the race condition for the clients ObjectInputStream.
                        // No stream will ever be busy.
                        try{
                            List<Future<Client>> resultList = threadPoolExecutor.invokeAll(callables, 2, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("USERSET END");
                    }

                }

            }catch (InterruptedException  e){
                e.printStackTrace();
            }
        }

    }


    /**
     * Runnable to send update UserSet representing a list of online clients
     */
    private class ListSender implements Runnable{
        private Sendables set;
        private Client client;
        public ListSender(Sendables set, Client client){
            this.set = set;
            this.client = client;
        }
        @Override
        public void run() {
            String thread = Thread.currentThread().getName();
            System.out.println("LISTSENDER " + thread);
            try {
                ObjectOutputStream oos;
                oos = client.getOos();
                oos.writeObject(set);
                oos.flush();
                oos.reset();

            } catch (IOException  e) {
                String logClientUpdateException = thread + " " + " ";
                logger.logEvent(Level.WARNING, logClientUpdateException, LocalTime.now());
                e.printStackTrace();
            }
            System.out.println("LISTSENDER END " + thread);
        }
    }

    private String getThreadName(Thread thread){
        return thread.getName().toString();
    }
}