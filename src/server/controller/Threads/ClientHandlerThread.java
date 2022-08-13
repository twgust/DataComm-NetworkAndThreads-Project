package server.controller.Threads;

import entity.Message;
import server.Entity.Client;
import server.ServerInterface.MessageReceivedEvent;

import server.ServerInterface.UserConnectionEvent;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * @author twgust
 * <p>
 * ClientHandler
 * Runs on its own Thread "ClientHandler", fetches an element from the buffer (queue)
 * The important difference between ClientHandlers buffer and Client buffer lies in data structure choice
 * <p>
 * ClientBuffer is used for look up and consists of a hashmap<User,Client>
 * ClientHandler is a FIFO queue which handles and assigns a thread to
 * clients as they establish Connection
 */
public class ClientHandlerThread {
    private final ThreadAssigner clientHandlerThread;
    private final LinkedList<Client> queue;
    private final ServerLogger logger;

    private ThreadPoolExecutor clientHandlerMainExec;
    private ThreadPoolExecutor clientHandlerThreadPool;

    private final MessageReceivedEvent messageReceivedEvent;
    private final UserConnectionEvent userConnectionEvent;

    public ClientHandlerThread(ServerLogger logger, MessageReceivedEvent messageReceivedEvent, UserConnectionEvent userConnectionEvent) {
        queue = new LinkedList<>();
        clientHandlerThread = new ThreadAssigner();
        this.messageReceivedEvent = messageReceivedEvent;
        this.userConnectionEvent = userConnectionEvent;
        this.logger = logger;
    }

    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPool) {
        this.clientHandlerThreadPool = threadPool;
        assert this.clientHandlerThreadPool != null;
        return this.clientHandlerThreadPool.getCorePoolSize() == 0;
    }

    public boolean setSingleThreadExecutor(ThreadPoolExecutor mainThread) {
        this.clientHandlerMainExec = mainThread;
        assert this.clientHandlerMainExec != null;
        return this.clientHandlerMainExec.getCorePoolSize() <= 1;
    }

    public void startClientHandler() {
        clientHandlerMainExec.execute(clientHandlerThread);
    }

    /**
     * Queues a client according to fifo principles
     *
     * @param client
     */
    public synchronized void queueClientForProcessing(Client client) {
        queue.addLast(client);
        ClientHandlerThread.this.notify();
    }

    /**
     * @return process client according to FIFO principle
     * @throws InterruptedException wait can throw interrupted-exception of thread is interrupted
     */
    private synchronized Client processClient() throws InterruptedException {
        while (queue.isEmpty()) {
            ClientHandlerThread.this.wait();
        }
        return queue.removeFirst();
    }


    /**
     * @author twgust
     * Thread for assigning a messageReceiver to each connected client
     * Thread is never freed unless server closes connection
     */
    private class ThreadAssigner implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String thread = Thread.currentThread().getName();
                    //log the event
                    String logThreadAssignerWaitingMsg = "Executing -> [TASK: Assign-Thread, STATE:RUNNING]" +
                            "\n>ready for new client!";
                    logger.logEvent(Level.INFO, thread, logThreadAssignerWaitingMsg, LocalTime.now());

                    // fetch client from front of queue
                    Client client = processClient();
                    String ip = "[" + client.getSocket().getLocalAddress().toString() + ":" + client.getSocket().getLocalPort() + "]";

                    // debug
                    // assign a thread to the client which will listen to incoming messages
                    MessageReceiverThread receiver = new MessageReceiverThread(client, logger);
                    clientHandlerThreadPool.submit(receiver);
                    receiver.addListener(messageReceivedEvent);


                    // log the event
                    String logThreadAssignedMsg = "Executed -> [Task: Assign-Thread" + client.getUser() + ", STATE:FINISHED]" +
                            "\n>assigned MessageReceiver-Thread to client @ " + ip;

                    logger.logEvent(Level.INFO, thread, logThreadAssignedMsg, LocalTime.now());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @author twgust
     */
    private class MessageReceiverThread implements Runnable {
        private MessageReceivedEvent listener;
        private final ServerLogger logger;
        private final String ip;
        private final ObjectInputStream ois;
        private final Client client;

        /**
         * @param client Client which is assigned the thread
         * @param logger logger for gui and .txt file
         */
        public MessageReceiverThread(Client client, ServerLogger logger) {
            this.logger = logger;
            this.client = client;
            ip = "[" + client.getSocket().getLocalAddress() + ":" + client.getSocket().getLocalPort() + "]";
            ois = client.getOis();
        }

        public void addListener(MessageReceivedEvent listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Object o = ois.readObject();
                    if (o instanceof Message) {
                        String thread = Thread.currentThread().getName();
                        String logMessageReceived = "Executing -> [TASK: Receive-Message]" +
                                " >> new message received, " + "notifying controller...";
                        logger.logEvent(Level.INFO, thread, logMessageReceived, LocalTime.now());

                        Message message = (Message) o;
                        listener.onMessageReceivedEvent(message);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    if (e instanceof SocketException) {
                        System.out.println("closed");
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }
}
