package server.controller;

import entity.*;
import server.Entity.Client;
import server.ServerInterface.*;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.Threads.ClientHandlerThread;
import server.controller.Threads.Sender.ObjectSenderThread;
import server.controller.Threads.ServerConnection;
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
    private ServerConnection serverConnection;
    private ObjectSenderThread objectSenderThread;
    private ClientHandlerThread clientHandler;
    private UserSetProducer userSetProducer;

    // executors for threads & runnables
    private ThreadPoolExecutor clientHandlerSingleThread;
    private ThreadPoolExecutor masterThreadPool;
    private ThreadPoolExecutor messageReceiverThreadPool;
    private ThreadPoolExecutor objectSenderPool;
    private ThreadPoolExecutor serverConnSingleThread;
    // when calling a function of the reference the implementations fire


    private UserConnectionEvent userConnectionEvent;
    private UserSetProducedEvent userSetProducedEvent;
    private MessageReceivedEvent messageReceivedEvent;

    /**
     * @author twgust
     */
    public ServerController() throws IOException {
        sendablesBuffer = new SendablesBuffer();
        messageBuffer = new MessageBuffer();
        clientBuffer = new ClientBuffer();
        userBuffer = new UserBuffer();
        logger = new ServerLogger();
    }

    /**
     * @param messageReceivedEvent
     * @author twgust
     */
    public void addMessageReceivedListener(MessageReceivedEvent messageReceivedEvent) {
        this.messageReceivedEvent = messageReceivedEvent;
    }

    /**
     * @param userConnectionEvent implementation of UserConnectionCallBack Interface
     * @author twgust
     */
    public void addConnectionListener(UserConnectionEvent userConnectionEvent) {
        this.userConnectionEvent = userConnectionEvent;
    }

    /**
     * @param userSetProduced
     * @author twgust
     */
    public void addProducerListener(UserSetProducedEvent userSetProduced) {
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
     * @author twgust
     * Initializes required components and starts server
     * invoked by ServerGUI.
     */
    public void startServer() {
        registerCallbackListeners();
        userSetProducer = new UserSetProducer(logger, userBuffer, userSetProducedEvent);
        serverConnection = new ServerConnection(logger, clientBuffer, userConnectionEvent);
        clientHandler = new ClientHandlerThread(logger, messageReceivedEvent, userConnectionEvent);
        objectSenderThread = new ObjectSenderThread(logger, sendablesBuffer, clientBuffer, userBuffer, messageBuffer, userConnectionEvent);
        configureExecutors();


        serverConnection.startServer();
        clientHandler.start();
        objectSenderThread.start();

        }


    /**
     * @author twgust
     * sets up executors which will run the different modules in the server.
     * Allows for higher level of control since the processes within a module can be individually killed as necessary.
     * <p>
     * 1) MasterThread pool invoked functions in the controller which starts some work in one of the threads,
     * typically when events are being called through callback interfaces, E.g. OnUserDisconnect/OnMessageReceived
     * <p>
     * 2) ServerThreadPool - Single Thread - Responsible for starting server and accepting incoming connections.
     * <p>
     * 3) ClientHandler - Single Thread - MainThread executes Runnable,
     * 3.1) ClientHandler - Thread Pool - ThreadPool, one thread for each connected client,
     * each thread in pool listens to its own client through the MessageReceiver.
     * <p>
     * 4) Object sender has a thread pool which it uses to concurrently send messages to clients
     */
    private void configureExecutors() {
        masterThreadPool = createThreadPool("ThreadPool - MASTER", 0, 50, new SynchronousQueue<>());
        objectSenderPool = createThreadPool("ThreadPool - ObjectSender", 0, 50, new SynchronousQueue<>());
        serverConnSingleThread = createThreadPool("ServerThread - Main", 1, 1, new LinkedBlockingQueue<>());
        clientHandlerSingleThread = createThreadPool("SingleThread - ClientHandlerMain", 1, 1, new LinkedBlockingQueue<>());
        messageReceiverThreadPool = createThreadPool("ThreadPool - MessageReceiver", 0, 25, new SynchronousQueue<>());

        serverConnection.setSingleThreadExecutor(serverConnSingleThread);
        clientHandler.setSingleThreadExecutor(clientHandlerSingleThread);
        clientHandler.setThreadPoolExecutor(messageReceiverThreadPool);
        objectSenderThread.setThreadPoolExecutor(objectSenderPool);
        userSetProducer.setSingleThreadExecutor(masterThreadPool);
    }

    /**
     * @param name         name of thread
     * @param corePoolSize size of threads to keep active in pool whether they're doing work, defaults to 0
     * @param max          max amount of threads at any given point
     * @param queue        the queue implementation provided
     * @return a ThreadPoolExecutor (Single/pool)
     * @author twgust
     * Custom implementation of executor service, allows for naming of threads in threadpool
     */
    public ThreadPoolExecutor createThreadPool(String name, int corePoolSize, int max, BlockingQueue<Runnable> queue) {
        return new ThreadPoolExecutor(corePoolSize, max, 60, TimeUnit.SECONDS, queue, new ThreadFactory() {
            final AtomicInteger integer = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, " <<THREAD: " + name + "[t=" + integer.getAndIncrement() + "]>> ");
            }
        });
    }

    /**
     * @author twgust
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * Every connected client is updated when fired.
     */
    @Override
    public void onUserConnectListener(User user) {
        synchronized (this) {
            Client client = clientBuffer.get(user);
            System.out.println("okokokOK!");
            String logUserConnectionMsg = Thread.currentThread().getName() +
                    "\nUser:" + user.getUsername() + "connected to the server!\n" +
                    "updating UserSet and enqueuing client for processing";
            logger.logEvent(Level.INFO, logUserConnectionMsg, LocalTime.now());
            userBuffer.put(user);
            masterThreadPool.submit(() -> userSetProducer.updateUserSet(user, ConnectionEventType.Connected));
            masterThreadPool.submit(() -> clientHandler.queueClientForProcessing(client));
        }
    }

    /**
     * @param user the client whose socket was closed.
     * @author twgust
     */
    @Override
    public void onUserDisconnectListener(User user) {
        synchronized (this) {
                try {
                    System.out.println("ONUSERDISCONNECT START");
                    String userDisconnectMsg = Thread.currentThread().getName()
                            + "\nUser: " + user.getUsername() + " disconnected from the server!";
                    logger.logEvent(Level.WARNING, userDisconnectMsg, LocalTime.now());

                    userBuffer.remove(user);
                    clientBuffer.removeUser(user);
                    userSetProducer.updateUserSet(user, ConnectionEventType.Disconnected);

                    String additionalDisconnectMsg = Thread.currentThread().getName() + "\nremoved [" + user + "] from buffers...";
                    logger.logEvent(Level.INFO, additionalDisconnectMsg, LocalTime.now());
                    System.out.println("ONUSERDISCONNECT END");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    /**
     * @param userSet the updated list containing all currently online clients
     * @author twgust
     */
    @Override
    public void userSetProduced(UserSet userSet) {
        synchronized (this) {
            masterThreadPool.submit(() -> {
                logger.logEvent(Level.INFO, Thread.currentThread().getName() + "> UserSet produced, updating clients!", LocalTime.now());
                try {
                    // here we use put first to prioritize the UserSet > messages
                    sendablesBuffer.putFirst(userSet);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            });
        }
    }

    /**
     * @param message
     * @author twgust
     */
    @Override
    public void onMessageReceivedEvent(Message message) {
        synchronized (this) {
            masterThreadPool.submit(() -> {
                try {
                    logger.logEvent(Level.INFO, Thread.currentThread().getName() +
                            " enqueuing message to sendables buffer" +
                            "\n" + message, LocalTime.now());
                    sendablesBuffer.enqueueSendable(message);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
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
}
