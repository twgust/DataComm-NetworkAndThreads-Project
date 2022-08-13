package server.controller.Threads.Sender;

import entity.*;
import server.Entity.Client;
import server.ServerInterface.UserConnectionEvent;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.ServerLogger;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
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
    public synchronized void startObjectSender(){
        threadPoolExecutor.submit(()->{
            start();
        });
    }

    private synchronized void addUnsendableToBuffer(User user, Message message) {
        ArrayList<User> recipient = new ArrayList<>();
        recipient.add(user);
        Message newMessage = null;
        switch (message.getType()) {
            case TEXT -> {
                newMessage = new Message(message.getTextMessage(), message.getAuthor(), recipient, MessageType.TEXT);
                messageBuffer.put(newMessage, user);
            }
            case IMAGE ->{
                newMessage = new Message(message.getImage(), message.getAuthor(), recipient, MessageType.IMAGE);
                messageBuffer.put(newMessage, user);
            }
            case TEXT_IMAGE -> {
                newMessage = new Message(message.getTextMessage(), message.getImage(), message.getAuthor(), recipient, MessageType.TEXT_IMAGE);
                messageBuffer.put(newMessage, user);
            }
        }
        System.out.println(newMessage);

    }
    public synchronized void start()   {
        while (sendablesBuffer != null) {
            try{

                Sendables sendable = sendablesBuffer.dequeueSendable();
                Message message = null;
                UserSet userSet = null;
                String thread = Thread.currentThread().getName();
                switch (sendable.getSendableType()) {
                    case Message -> {
                        message = (Message) sendable;
                        Message finalMessage = message;

                        String logSendMessageStart = "Executing --[TASK: Send-Message, {"+ message.hashCode() + "}]";
                        logger.logEvent(Level.INFO,thread, logSendMessageStart, LocalTime.now());

                        List<Client> list = new ArrayList<>();
                        message.getRecipientList().forEach(user -> {
                            if(clientBuffer.get(user) == null){
                                System.out.println("ok now we hre ");
                                addUnsendableToBuffer(user, finalMessage);
                            }
                            else{
                                list.add(clientBuffer.get(user));
                            }
                        });
                        System.out.println(list.size());
                        ArrayList<MessageCallable> callables = new ArrayList<>(list.size());
                        list.forEach(client -> {
                            MessageCallable callable = new MessageCallable(logger, finalMessage, client, listener, messageBuffer);
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
                        String logSendMessageEnd = "[TASK: Send-Message, {"+message.hashCode()+"}]" + ">Completed!";
                        logger.logEvent(Level.INFO,thread,logSendMessageEnd, LocalTime.now());
                        System.out.println("\n--DIVIDER--\n");
                    }

                    case UserSet -> {
                        System.out.println("USER UPDATE S");

                        userSet = (UserSet) sendable;
                        UserSet finalSet = userSet;
                        ArrayList<OnlineListCallable> callables = new ArrayList<>(clientBuffer.size());

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
                        System.out.println("USER UPDATE E");
                    }

                }

            }catch (InterruptedException  e){
                e.printStackTrace();
            }
        }

    }
    private String getThreadName(Thread thread){
        return thread.getName().toString();
    }
}