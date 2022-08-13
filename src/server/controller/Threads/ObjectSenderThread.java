package server.controller.Threads;

import entity.*;
import server.Entity.Client;
import server.ServerInterface.UserConnectionEvent;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.ServerLogger;
import server.controller.Threads.Callable.MessageCallable;
import server.controller.Threads.Callable.OnlineListCallable;

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
    private final SendablesBuffer sendablesBuffer;

    private ThreadPoolExecutor threadPoolExecutor;
    private UserConnectionEvent listener;

    /**
     * Constructor,
     *
     * @param logger
     * @param clientBuffer reference to buffer (final)
     * @param userBuffer   reference to buffer (final)
     */
    public ObjectSenderThread(ServerLogger logger, SendablesBuffer buffer, ClientBuffer clientBuffer,
                              MessageBuffer messageBuffer, UserConnectionEvent userConnectionEvent) {
        this.logger = logger;
        this.messageBuffer = messageBuffer;
        this.listener = userConnectionEvent;
        this.clientBuffer = clientBuffer;

        this.sendablesBuffer = buffer;
    }

    /**
     * Sets executor, invoked by controller.
     * @param threadPoolExecutor has to be thread pool
     * @return
     */
    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        assert this.threadPoolExecutor != null;
        return true;
    }

    /**
     * Starts the message sender
     */
    public synchronized void startObjectSender() {
        threadPoolExecutor.submit(() -> {
            Sender();
        });
    }

    /**
     * Similar to callable implementation.
     * Takes a message and an author, remove all users from the recipient list except the User from the func parameter.
     * invoked in start();
     *
     * @param user    User which has disconnected / closed its stream/sock
     * @param message the original message.
     */
    private synchronized void addMessageToMsgBuffer(User user, Message message) {
        ArrayList<User> recipient = new ArrayList<>();
        recipient.add(user);
        Message newMessage = null;
        switch (message.getType()) {
            case TEXT -> {
                newMessage = new Message(message.getTextMessage(), message.getAuthor(), recipient, MessageType.TEXT);
                messageBuffer.put(newMessage, user);
            }
            case IMAGE -> {
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

    /**
     * @author twgust
     * Concurrent Message Sending while assuring that no stream is busy, one Thread per .writeObject() call.
     * A message object with 10 users in the recipientlist can therefore be sent out concurrently,
     * assuming 10 threads are free.
     * <p>
     * Prio is UserSet > Message
     */
    public synchronized void Sender() {
        while (sendablesBuffer != null) {
            try {
                // get sendable object by polling it from queue
                Sendables sendable = sendablesBuffer.dequeueSendable();
                Message message = null;
                UserSet userSet = null;
                String thread = Thread.currentThread().getName();

                // determine type of Sendable (UserSet/Message)
                switch (sendable.getSendableType()) {
                    case Message -> {
                        message = (Message) sendable;
                        Message finalMessage = message;

                        String logSendMessageStart = "Executing ->[TASK: Send-Message, {" + message.hashCode() + "}]";
                        logger.logEvent(Level.INFO, thread, logSendMessageStart, LocalTime.now());

                        // for each recipient, get the associated client object from buffer
                        List<Client> list = new ArrayList<>();
                        message.getRecipientList().forEach(user -> {
                            // if the client is null, add the unsendable message to the buffer,
                            // the message will be sent once the null client reconnects
                            if (clientBuffer.get(user) == null) {
                                addMessageToMsgBuffer(user, finalMessage);
                            }
                            // else, the client is connected and we add it to the list of clients
                            else {
                                list.add(clientBuffer.get(user));
                            }
                        });

                        ArrayList<MessageCallable> callables = new ArrayList<>(list.size());
                        // for each client in the list of clients, create a new Callable
                        list.forEach(client -> {
                            MessageCallable callable = new MessageCallable(logger, finalMessage, client, listener, messageBuffer);
                            callables.add(callable);
                        });
                        // invoke all the callables and fetch the result in a list of Future<Client>
                        List<Future<Client>> resultList = threadPoolExecutor.invokeAll(callables, 2, TimeUnit.SECONDS);
                        for (int i = 0; i < resultList.size(); i++) {
                            Future<Client> clientel = resultList.get(i);
                            try {
                                // here we see if an exception - that wasn't handled by the Callable - occurrs.
                                Client client = clientel.get();
                            } catch (ExecutionException e) {
                                System.out.println(e.getCause());
                                e.printStackTrace();
                            }
                        }
                        // all online users on the recipient list has been sent a message by this point!
                        String logSendMessageEnd = "[TASK: Send-Message, {" + message.hashCode() + "}]" + "  >> Completed!";
                        logger.logEvent(Level.INFO, thread, logSendMessageEnd, LocalTime.now());
                    }

                    case UserSet -> {
                        userSet = (UserSet) sendable;
                        UserSet finalSet = userSet;
                        ArrayList<OnlineListCallable> callables = new ArrayList<>(clientBuffer.size());

                        // set up a collection of all the clients
                        Collection<Client> arr = clientBuffer.allValues();
                        // for each client c,
                        // create a OnlineListCallable(ServerLogger,Sendable, Client c) obj
                        // add the instance to a list of callables.
                        for (Client client : arr) {
                            OnlineListCallable callable = new OnlineListCallable(logger, finalSet, client);
                            callables.add(callable);
                        }
                        // execute all the callables and wait for result, this way we achieve concurrency in message sending
                        // while avoiding the race condition for the clients ObjectInputStream.
                        // No stream will ever be busy.
                        try {
                            List<Future<Client>> resultList = threadPoolExecutor.invokeAll(callables, 2, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private String getThreadName(Thread thread) {
        return thread.getName().toString();
    }
}