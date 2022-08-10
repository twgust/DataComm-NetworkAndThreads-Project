package server.controller;

import entity.*;
import server.ServerInterface.*;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.Threads.ClientHandlerThread;
import server.controller.Threads.Sender.ObjectSenderThread;
import server.controller.Threads.OpenServerConnection;
import server.controller.Threads.UserSetProducer;

import java.io.*;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;


/**
 * Notes and Requirements for Server:
 * <p>
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 * <p>
 * ----------------------------------------------------------------------------*
 * NonFunctional (qualitative) Requirements to implement for Server:
 * <p>
 * 1, Handle a large amount of clients
 * ----------------------------------------------------------------------------*
 * <p>
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Server:
 * <p>
 * 1, Allow for a Client to connect to the server [X]
 * 2, Allow for a Client to disconnect from the server [X]
 * <p>
 * <p>
 * 3, Keep an updated List of currently connected Clients in some Data Structure [X]
 * 3.1 Operations on the Data Structure need be Synchronized [X]
 * <p>
 * <p>
 * 4, Store a Messages that can't be sent in some Data Structure [x]
 * 4.1 Operations on the Data Structure need be Synchronized [x]
 * 4.2 On Client reconnect, send the stored Message []
 * <p>
 * 5, Log all traffic on the server to a locally stored File [x]
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) [x] Partial
 * ----------------------------------------------------------------------------*
 */

public class ServerController implements UserConnectionEvent, MessageReceivedEvent, UserSetProducedEvent {
    // Buffers
    private final SendablesBuffer sendablesBuffer;
    private final MessageBuffer messageBuffer;
    private final ClientBuffer clientBuffer;
    private final UserBuffer userBuffer;
    private final ServerLogger logger;
    // server threads
    private OpenServerConnection openServerConnection;
    private ObjectSenderThread objectSenderThread;
    private ClientHandlerThread clientHandler;
    private UserSetProducer userSetProducer;

    // executors for threads & runnables
    private ThreadPoolExecutor clientHandlerSingleThread;
    private ThreadPoolExecutor masterThreadPool;
    private ThreadPoolExecutor clientHandlerPool;
    private ThreadPoolExecutor objectSenderPool;
    private ThreadPoolExecutor server;
    // when calling a function of the reference the implementations fire


    private UserConnectionEvent userConnectionEvent;
    private UserSetProducedEvent userSetProducedEvent;
    private MessageReceivedEvent messageReceivedEvent;

    /**
     * Constructor
     */
    public ServerController() throws IOException {
        sendablesBuffer = new SendablesBuffer();
        messageBuffer = new MessageBuffer();
        clientBuffer = new ClientBuffer();
        userBuffer = new UserBuffer();
        logger = new ServerLogger();
    }

    public void addMessageReceivedListener(MessageReceivedEvent messageReceivedEvent) {
        this.messageReceivedEvent = messageReceivedEvent;
    }
    /**
     * @param userConnectionEvent implementation of UserConnectionCallBack Interface
     */
    public void addConnectionListener(UserConnectionEvent userConnectionEvent) {
        this.userConnectionEvent = userConnectionEvent;
    }
    public void addProducerListener(UserSetProducedEvent userSetProduced){
        this.userSetProducedEvent = userSetProduced;
    }

    /**
     * @param impl
     */
    public void sendLoggerCallbackImpl(LoggerCallBack impl) {
        logger.setLoggerCallback(impl);
    }


    /**
     * should more callback listeners be implemented
     */
    private void registerCallbackListeners() {
        addMessageReceivedListener(this);
        addConnectionListener(this);
        addProducerListener(this);
    }

    /**
     * Initializes required components and starts server
     * invoked by ServerGUI.
     */
    public void startServer()  {
        registerCallbackListeners();
        openServerConnection = new OpenServerConnection(logger, clientBuffer, userConnectionEvent);
        clientHandler = new ClientHandlerThread(logger, messageBuffer, messageReceivedEvent);
        objectSenderThread = new ObjectSenderThread(logger, sendablesBuffer,clientBuffer, userBuffer, messageBuffer);
        userSetProducer  = new UserSetProducer(userBuffer,userSetProducedEvent);

        configureExecutors();


        openServerConnection.startServer();
        clientHandler.start();
        userSetProducer.start();
        objectSenderThread.start();
    }

    /**
     * sets up executors which will run the different modules in the server
     */
    private void configureExecutors() {
        masterThreadPool = createThreadPool("ThreadPool - MASTER", 0,10,new SynchronousQueue<>());


        server = createThreadPool("ServerThread - Main", 1, 1, new LinkedBlockingQueue<>());
        openServerConnection.setSingleThreadExecutor(server);

        clientHandlerSingleThread = createThreadPool("SingleThread - ClientHandlerMain", 1, 1, new LinkedBlockingQueue<>());
        clientHandler.setSingleThreadExecutor(clientHandlerSingleThread);

        clientHandlerPool = createThreadPool("ThreadPool - ClientHandler", 0, 25, new SynchronousQueue<>());
        clientHandler.setThreadPoolExecutor(clientHandlerPool);

        objectSenderPool = createThreadPool("ThreadPool - ObjectSender", 0, 25, new SynchronousQueue<>());
        objectSenderThread.setThreadPoolExecutor(objectSenderPool);

    }

    public ThreadPoolExecutor createThreadPool(String name, int corePoolSize, int max, BlockingQueue<Runnable> queue) {
        return new ThreadPoolExecutor(corePoolSize, max, 60, TimeUnit.SECONDS, queue, new ThreadFactory() {
            final AtomicInteger integer = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, name + "[t=" + integer.getAndIncrement() + "]");
            }
        });
    }

    /**
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * Every connected client is updated when fired.
     */
    @Override
    public void onUserConnectListener(User user) {
        synchronized (this){
            masterThreadPool.submit(() -> userSetProducer.queueUserSetProduction(user, ConnectionEventType.Connected));
            masterThreadPool.submit(() -> clientHandler.queueClientForProcessing(clientBuffer.get(user)));
        }

    }

    /**
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
        synchronized (this) {

        }
    }

    @Override
    public void userSetProduced(UserSet userSet) {
        synchronized (this) {
                masterThreadPool.submit(()-> {
                    try {
                        sendablesBuffer.enqueueSendable(userSet);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    @Override
    public void onMessageReceivedEvent(Message message) {
        synchronized (this) {
            try {
                sendablesBuffer.enqueueSendable(message);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * WIP
     * Function for handling Socket exceptions
     *
     * @param e      the exception to be handled
     * @param thread the thread in which the Exception occurred
     */
    private void handleIOException(IOException e, String methodName, Thread thread, User user, String clientIP) throws IOException {
        if (clientIP.isEmpty()) {
            clientIP = "exception occurred in unknown client";
        }
        if (e instanceof SocketException) {
            // assume user disconnect, at any rate the SocketException has the effect of a disconnected user
            // log to server gui

            // onUserDisconnectListener(user, ); fix this
        } else if (e instanceof EOFException) {
            // log to server gui
            String logEndOfFileMsg = "Client: EOF exception for " + user.getUsername() + " in " + methodName;
            logger.logEvent(Level.WARNING, logEndOfFileMsg, LocalTime.now());
        } else {
            // log to server gui
            String logUnhandledExceptionMsg = "UNHANDLED EXCEPTION\n" + e.getMessage();
            logger.logEvent(Level.WARNING, logUnhandledExceptionMsg, LocalTime.now());
        }
    }

    /**
     * Runnable for closing the server
     */
    private class ServerDisconnect implements Runnable {
        @Override
        public void run() {

        }
    }
}
