package server.controller.threads;

import entity.Message;
import server.entity.Client;
import server.serverinterface.MessageReceivedEvent;

import server.serverinterface.UserConnectionEvent;
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
 * ClientHandler
 * Runs on its own Thread "ClientHandler"
 * The important difference between ClientHandlers buffer and Client buffer lies in data structure choice
 * ClientBuffer is used for look up and consists of a hashmap<User,Client>
 * ClientHandler is a FIFO queue which handles and assigns a MessageReceiver Thread to clients as they establish Connection
 *
 * flow: Controller --(starts)--> ClientHandler --(starts)--> ThreadAssigner --(starts)--> MessageReceiver
 */
public class ClientHandlerThread {
    private final ThreadAssigner clientHandlerThread;
    private final LinkedList<Client> queue;
    private final ServerLogger logger;

    private ThreadPoolExecutor clientHandlerMainExec;
    private ThreadPoolExecutor clientHandlerThreadPool;

    private final MessageReceivedEvent messageReceivedEvent;
   // private final UserConnectionEvent userConnectionEvent;

    /**
     *
     * @param logger ServerLogger
     * @param messageReceivedEvent implementation from Controller
     * @param userConnectionEvent implementation from Controller
     */
    public ClientHandlerThread(ServerLogger logger, MessageReceivedEvent messageReceivedEvent, UserConnectionEvent userConnectionEvent) {
        queue = new LinkedList<>();
        clientHandlerThread = new ThreadAssigner();
        this.messageReceivedEvent = messageReceivedEvent;
       // this.userConnectionEvent = userConnectionEvent;
        this.logger = logger;
    }

    /**
     * @param threadPool
     * @return true if max threads is larger than 1 (ThreadPool)
     */
    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPool) {
        this.clientHandlerThreadPool = threadPool;
        assert this.clientHandlerThreadPool != null;
        return this.clientHandlerThreadPool.getMaximumPoolSize() > 1;
    }

    /**
     * @param mainThread
     * @return true if max threads == 1 (SingleThreadExecutor)
     */
    public boolean setSingleThreadExecutor(ThreadPoolExecutor mainThread) {
        this.clientHandlerMainExec = mainThread;
        assert this.clientHandlerMainExec != null;
        return this.clientHandlerMainExec.getMaximumPoolSize() == 1;
    }

    /**
     * Starts the underlying ClientHandler Runnable "ThreadAssigner"
     */
    public void startClientHandler() {
        clientHandlerMainExec.execute(clientHandlerThread);
    }

    /**
     * Queues a client according to fifo principles
     * @param client client to be queued
     */
    public synchronized void queueClientForProcessing(Client client) {
        queue.addLast(client);
        ClientHandlerThread.this.notify();
    }

    /**
     * @return process client according to FIFO principle
     * @throws InterruptedException wait can throw interrupted-exception of thread is interrupted
     */
    private synchronized Client processNextClient() throws InterruptedException {
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
                            " >> Assigning client a MessageReceiver Thread!";
                    logger.logEvent(Level.INFO, thread, logThreadAssignerWaitingMsg, LocalTime.now());

                    // fetch client from front of queue
                    Client client = processNextClient();
                    String ip = "[" + client.getSocket().getLocalAddress().toString() + ":" + client.getSocket().getLocalPort() + "]";

                    // debug
                    // assign a thread to the client which will listen to incoming messages
                    MessageReceiverThread receiver = new MessageReceiverThread(client, logger);
                    clientHandlerThreadPool.submit(receiver);
                    receiver.addListener(messageReceivedEvent);


                    // log the event
                    String logThreadAssignedMsg = "Executed -> [Task: Assign-Thread" + client.getUser() + ", STATE:FINISHED]" +
                            " >> Assigned MessageReceiver-Thread to client @ " + ip;

                    logger.logEvent(Level.INFO, thread, logThreadAssignedMsg, LocalTime.now());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * One MessageReceiverThread for each Connected Client.
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
