package server.controller.Threads;

import entity.Message;
import server.Entity.Client;
import server.ServerInterface.MessageReceivedEvent;

import server.ServerInterface.UserConnectionEvent;
import server.controller.ServerLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * ClientHandler
 * Runs on its own Thread "ClientHandler", fetches an element from the buffer
 * The important difference between ClientHandlers buffer and Client buffer lies in data structure choice
 *
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
    public ClientHandlerThread(ServerLogger logger, MessageReceivedEvent messageReceivedEvent, UserConnectionEvent userConnectionEvent){
        queue = new LinkedList<>();
        clientHandlerThread = new ThreadAssigner();
        this.messageReceivedEvent = messageReceivedEvent;
        this.userConnectionEvent = userConnectionEvent;
        this.logger = logger;
    }

    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPool){
        this.clientHandlerThreadPool = threadPool;
        assert this.clientHandlerThreadPool != null;
        return this.clientHandlerThreadPool.getCorePoolSize() == 0;
    }
    public boolean setSingleThreadExecutor(ThreadPoolExecutor mainThread){
        this.clientHandlerMainExec = mainThread;
        assert this.clientHandlerMainExec != null;
        return this.clientHandlerMainExec.getCorePoolSize() <= 1;
    }

    public void start(){
        clientHandlerMainExec.execute(clientHandlerThread);
    }

    /**
     * Queues a client according to fifo principles
     * @param client
     */
    public synchronized void queueClientForProcessing(Client client){
        queue.addLast(client);
        ClientHandlerThread.this.notify();
    }

    /**
     * @return process client according to FIFO principle
     * @throws InterruptedException wait can throw interrupted-exception of thread is interrupted
     */
    private synchronized Client processClient() throws InterruptedException  {
        while(queue.isEmpty()) {
            ClientHandlerThread.this.wait();
        }
        return queue.removeFirst();
    }



    /**
     * Thread for assigning a messageReceiver to each connected client
     * Thread is never freed unless server closes connection
     */
    private class ThreadAssigner implements Runnable{
        @Override
        public void run() {
            while(true){
                try{
                    //log the event
                    String logThreadAssignerWaitingMsg = Thread.currentThread().getName() + " awaiting client to process...";
                    logger.logEvent(Level.INFO, logThreadAssignerWaitingMsg, LocalTime.now());

                    // fetch client from front of queue
                    Client client = processClient();

                    // debug
                    // assign a thread to the client which will listen to incoming messages
                    MessageReceiverThread receiver = new MessageReceiverThread(client, logger);
                    clientHandlerThreadPool.submit(receiver);
                    receiver.addListener(messageReceivedEvent);


                    // log the event
                    String logThreadAssignedMsg = Thread.currentThread().getName() + " Assigned MessageReceiver Thread to client @ "
                            + client.getSocket().getRemoteSocketAddress().toString();
                    logger.logEvent(Level.INFO, logThreadAssignedMsg, LocalTime.now());

                } catch (InterruptedException  e){e.printStackTrace();}
            }
        }
    }
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
        public MessageReceiverThread(Client client, ServerLogger logger ) {
            this.logger = logger;
            this.client = client;
            ip = client.getSocket().getRemoteSocketAddress().toString();
            ois = client.getOis();
        }
        public void addListener(MessageReceivedEvent listener){
            this.listener = listener;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    Object o = ois.readObject();
                    if (o instanceof Message) {
                        Message message = (Message) o;
                        String logMessageReceived = Thread.currentThread().getName()
                                + "\n--Message received from @ " + ip + "!"
                                + "\n---" + message.getAuthor() + " " + message.getType();
                        logger.logEvent(Level.INFO, logMessageReceived, LocalTime.now());
                        listener.onMessageReceivedEvent(message);
                    }
                } catch (ClassNotFoundException | IOException e) {
                    userConnectionEvent.onUserDisconnectListener(client.getUser());
                    break;
                }
            }
        }
    }
}
