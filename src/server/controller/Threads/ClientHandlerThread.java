package server.controller.Threads;

import server.Entity.Client;
import server.ServerInterface.MessageReceivedEvent;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.ServerLogger;

import java.time.LocalTime;
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
    private final MessageBuffer messageBuffer;
    private final LinkedList<Client> queue;
    private final ServerLogger logger;

    private ThreadPoolExecutor clientHandlerMainExec;
    private ThreadPoolExecutor clientHandlerThreadPool;

    private MessageReceivedEvent messageReceivedEvent;


    public ClientHandlerThread(ServerLogger logger , MessageBuffer messageBuffer, MessageReceivedEvent event){
        this.logger = logger;
        this.messageBuffer = messageBuffer;
        this.messageReceivedEvent = event;

        queue = new LinkedList<>();
        clientHandlerThread = new ThreadAssigner();
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
                    MessageReceiverThread receiver = new MessageReceiverThread(client, logger, messageBuffer);
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
}
