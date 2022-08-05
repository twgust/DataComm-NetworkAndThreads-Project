package server.controller;

import entity.HandledUserType;
import entity.Message;
import entity.User;
import entity.UserSet;
import server.Entity.Client;
import server.ServerInterface.LoggerCallBack;
import server.ServerInterface.MessageReceivedListener;
import server.ServerInterface.UserConnectionCallback;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.Threads.ServerMainThread;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * 4, Store a Messages that can't be sent in some Data Structure []
 * 4.1 Operations on the Data Structure need be Synchronized []
 * 4.2 On Client reconnect, send the stored Message []
 * <p>
 * 5, Log all traffic on the server to a locally stored File []
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) [x] Partial
 * ----------------------------------------------------------------------------*
 */

public class ServerController implements UserConnectionCallback, MessageReceivedListener {

    // server
    private final ClientBuffer clientBuffer;
    private final UserBuffer userBuffer;

    // server threads
    private ExecutorService threadPool;
    private ExecutorService serverExecutor;

    private ServerMainThread serverMainThread;

    private ServerLogger logger;

    // when calling a function of the reference the implementations fire
    private LoggerCallBack loggerCallback;
    private UserConnectionCallback userConnectionCallback;
    private MessageReceivedListener messageReceivedCallback;
    private ArrayList<MessageReceivedListener> messageReceivedListeners;

    /**
     * Constructor
     * */
    public ServerController() throws IOException {
        clientBuffer = new ClientBuffer();
        userBuffer = new UserBuffer();
        logger = new ServerLogger();
    }

    /**
     * List of implementations so ServerGUI and ServerController can provide their own impl.
     *
     * @param connectionListener implementation of UserConnectionCallBack Interface
     */
    public void addConnectionListener(UserConnectionCallback connectionListener) {
        this.userConnectionCallback = connectionListener;
    }

    public void addMessageReceivedListener(MessageReceivedListener messageListener) {
        this.messageReceivedCallback = messageListener;
    }

    public void addMessageReceivedListenerThread(MessageReceivedListener listener) {
        if (messageReceivedListeners == null) {
            messageReceivedListeners = new ArrayList<>();
        }
        messageReceivedListeners.add(listener);
    }

    public void sendLoggerCallbackImpl(LoggerCallBack impl) {
        logger.setLoggerCallback(impl);
    }

    /**
     * should more callback listeners be implemented
     */
    private void registerCallbackListeners() {
        addConnectionListener(this);
        addMessageReceivedListener(this);
    }

    /**
     * Starts and initializes server,
     * invoked by ServerGUI.
     */
    public void startServer() {
        registerCallbackListeners();



        // start Server
        serverExecutor = Executors.newSingleThreadExecutor();
        threadPool = Executors.newCachedThreadPool();
        serverMainThread = new ServerMainThread(clientBuffer, logger, userConnectionCallback);
        serverExecutor.execute(serverMainThread);
    }

    /**
     * Closes servers and all client connections,
     * invoked by ServerGUI.
     */
    public void stopServer() {
        ServerDisconnect disconnect = new ServerDisconnect();
    }

    /**
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * Every connected client is updated when fired.
     * @param connectedClient connected Client
     */
    @Override
    public void onUserConnectListener(User connectedClient) {
        //submit one of the threads from pool to the synch method
        threadPool.submit(()-> {
            try {
                updateOnlineUsers(connectedClient, HandledUserType.Connected);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * TODO fix this
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
        //submit one of the threads from pool to the synch method

        threadPool.submit(() -> {
            try {
                updateOnlineUsers(disconnectedClient, HandledUserType.Disconnected);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * @param message
     * @param client
     * @param oos
     */
    @Override
    public void onMessageReceived(Message message, Socket client, ObjectOutputStream oos) {
        threadPool.execute(() -> {
            try {
                logger.logEvent(Level.INFO, "message received from: " + message.getAuthor() + " [" + message.getTextMessage() + "]", LocalTime.now());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            for (User user : message.getRecipientList()) {
                new Sender(message, client, oos).run();
            }
        });


    }

    private void handleInterruptedException(Exception e) {

    }
    private synchronized void updateOnlineUsers(User user, HandledUserType type) throws IOException {
        switch (type){
            case Connected -> {
                String logOnUserConnectMsg;
                logOnUserConnectMsg = "adding " + user + " to list of online clients";
                logger.logEvent(Level.INFO, logOnUserConnectMsg, LocalTime.now());

                // put the connected client in the user buffer (HashSet<User>)
                userBuffer.put(user);

                // fetch the userHashSet, userBuffer is synchronized and thread-safe
                HashSet<User> userHashSet = userBuffer.getUserBuffer();

                // for each connected client,
                // send an instance of the  UserSet Class (<HashSet<User> set, User connectedClient);
                clientBuffer.getKeySet().parallelStream().forEach(u -> {
                    try {
                        // for each user in buffer, update their "online list"
                        // Runnable still runs on thread assigned at start of func pool.
                        new Sender(new UserSet(userHashSet, user, HandledUserType.Connected),
                                u, clientBuffer.get(u).getSocket(), clientBuffer.get(u).getOos()).run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }

            case Disconnected ->
            {
                String logSocketExceptionMsg = user + " disconnected from the server";
                logger.logEvent(Level.WARNING, logSocketExceptionMsg, LocalTime.now());

                try {
                    userBuffer.remove(user);
                    clientBuffer.removeUser(user);
                }
                catch (Exception e) {e.printStackTrace();}
                HashSet<User> userHashSet = userBuffer.getUserBuffer();

                clientBuffer.getKeySet().parallelStream().forEach((u -> {
                    try{
                        Client client = clientBuffer.get(u);
                        new Sender(new UserSet(userHashSet, user, HandledUserType.Disconnected), u,
                                client.getSocket(), client.getOos()).run();}
                    catch (InterruptedException e) {e.printStackTrace();}
                }

                ));


            }
        }
    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class Sender implements Runnable {
        // update lists
        private UserSet objectOutSet;
        private User user; // recipient of message

        //send message
        private Message message;

        // socket and stream to send to
        private final Socket socket;
        private final ObjectOutputStream oos;


        /**
         * Constructor for sending messages to clients
         *
         * @param message message to be sent
         * @param socket  client which sent the request to server
         */
        public Sender(Message message, Socket socket, ObjectOutputStream oos) {
            this.message = message;
            this.socket = socket;
            this.oos = oos;
        }

        /**
         * Updates list of currently online users
         *
         * @param userSet the set of users + the recent disconnect/connected user as an obj
         * @param user    the user of the user which the update is going to be sent to,
         */
        public Sender(UserSet userSet, User user, Socket socket, ObjectOutputStream oos) {
            this.socket = socket;
            this.oos = oos;
            this.user = user;
            this.objectOutSet = userSet;
        }

        @Override
        public void run() {
            try {

                // 1)
                // the Client will receive a "UserSet" which includes:
                // a HashSet<User> of online users
                // a User object which represents the disconnected/Connected client in the UserConnectionInterface
                if (this.objectOutSet != null) {


                    oos.writeObject(objectOutSet);
                    oos.reset();
                    String str = "Sent UserSet:{'<Hashset<User>' size of set = " + objectOutSet.getUserSet().size() +", User: '" + objectOutSet.getHandledUser() + "'} " + "to --> " + user + " @" + socket.getInetAddress();
                    logger.logEvent(Level.INFO, str, LocalTime.now());
                }
                /**
                 * TODO
                 */
                else if (this.message != null) {
                    oos.writeObject(message);
                    oos.reset();
                    String logMessageSentMsg = "Message sent to user: [" + user + "] @IP: " + socket.getInetAddress().toString();
                    logger.logEvent(Level.INFO, logMessageSentMsg, LocalTime.now());
                }


            } catch (IOException e) {
                if (e instanceof SocketException) {

                    try {
                        handleIOException(e, "SendObject.run()", Thread.currentThread(), user, "");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    /**
     * Runnable for receiving a message from a client
     * Reads from connected clients ObjectInputStream.
     */
    private class ReceiveMessage implements Runnable {
        private final Client client;

        public ReceiveMessage(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    ObjectInputStream ois = client.getOis();
                    Object o = ois.readObject();
                    if (o instanceof Message) {
                        Message message = (Message) o;
                        messageReceivedCallback.onMessageReceived(message, client.getSocket(), client.getOos());
                    }

                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

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

            onUserDisconnectListener(user);
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
