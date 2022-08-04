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

    // when calling a function of the reference the implementations fire
    private LoggerCallBack loggerCallback;
    private UserConnectionCallback userConnectionCallback;
    private MessageReceivedListener messageReceivedCallback;
    private ArrayList<MessageReceivedListener> messageReceivedListeners;

    /**
     * Constructor
     * */
    public ServerController() {
        clientBuffer = new ClientBuffer();
        userBuffer = new UserBuffer();
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

    public void addLoggerCallbackImpl(LoggerCallBack impl) {
        this.loggerCallback = impl;
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
        serverMainThread = new ServerMainThread(clientBuffer, loggerCallback, userConnectionCallback);
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
        // logic executes in thread
        threadPool.submit((() -> {
            String logOnUserConnectMsg;
            logOnUserConnectMsg = "adding " + connectedClient + " to list of online clients";
            loggerCallback.logInfoToGui(Level.INFO, logOnUserConnectMsg, LocalTime.now());

            // put the connected client in the user buffer (HashSet<User>)
            userBuffer.put(connectedClient);

            // fetch the userHashSet, userBuffer is synchronized and thread-safe
            HashSet<User> userHashSet = userBuffer.getUserBuffer();

            // for each connected client,
            // send an instance of the  UserSet Class (<HashSet<User> set, User connectedClient);
            clientBuffer.getKeySet().parallelStream().forEach(u -> {
                try {
                    // for each user in buffer, update their "online list"
                    // Runnable still runs on thread assigned at start of func pool.
                    threadPool.execute(new SendObject(new UserSet(userHashSet, connectedClient, HandledUserType.Connected),
                            u, clientBuffer.get(u).getSocket(), clientBuffer.get(u).getOos()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }));
    }

    /**
     * TODO fix this
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
        threadPool.submit(() -> {
            String logOnUserDisconnectMessage = "removing " + disconnectedClient + " from list of online clients";
            loggerCallback.logInfoToGui(Level.WARNING, logOnUserDisconnectMessage, LocalTime.now());

            try { userBuffer.remove(disconnectedClient); }
            catch (Exception e) {e.printStackTrace();}
            HashSet<User> userHashSet = userBuffer.getUserBuffer();

            clientBuffer.getKeySet().parallelStream().forEach((u -> {
                try{
                    Client client = clientBuffer.get(u);
                    new SendObject(new UserSet(userHashSet, disconnectedClient, HandledUserType.Disconnected), u,
                            client.getSocket(), client.getOos()).run();}
                catch (InterruptedException e) {e.printStackTrace();}
            }));
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
            loggerCallback.logInfoToGui(Level.INFO, "message received from: " + message.getAuthor() + " [" + message.getTextMessage() + "]", LocalTime.now());

            for (User user : message.getRecipientList()) {
                new SendObject(message, client, oos).run();
            }
        });


    }

    private void handleInterruptedException(Exception e) {

    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendObject implements Runnable {
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
        public SendObject(Message message, Socket socket, ObjectOutputStream oos) {
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
        public SendObject(UserSet userSet, User user, Socket socket, ObjectOutputStream oos) {
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
                    String str = "Sending UserSet:{'<Hashset<User>', User: '" + objectOutSet.getHandledUser() + "'} " + "to --> " + user + " @" + socket.getInetAddress();
                    loggerCallback.logInfoToGui(Level.INFO, str, LocalTime.now());

                    oos.writeObject(objectOutSet);
                    oos.reset();
                }
                /**
                 * TODO
                 */
                else if (this.message != null) {

                }


            } catch (IOException e) {
                if (e instanceof SocketException) {
                    e.printStackTrace();
                    handleIOException(e, "SendObject.run()", Thread.currentThread(), user, "");
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
    private void handleIOException(IOException e, String methodName, Thread thread, User user, String clientIP) {
        if (clientIP.isEmpty()) {
            clientIP = "exception occurred in unknown client";
        }
        if (e instanceof SocketException) {
            // assume user disconnect, at any rate the SocketException has the effect of a disconnected user

            // fire the onUserDisconnect event for each implementation of the interface (Server Controller,GUI)

            // log to server gui
            String logSocketExceptionMsg = "Client IP: [" + clientIP + "]  USER:" + user + " disconnected";
            loggerCallback.logInfoToGui(Level.WARNING, logSocketExceptionMsg, LocalTime.now());
        } else if (e instanceof EOFException) {
            // log to server gui
            String logEndOfFileMsg = "Client: EOF exception for " + user.getUsername() + " in " + methodName;
            loggerCallback.logInfoToGui(Level.WARNING, logEndOfFileMsg, LocalTime.now());
        } else {
            // log to server gui
            String logUnhandledExceptionMsg = "UNHANDLED EXCEPTION\n" + e.getMessage();
            loggerCallback.logInfoToGui(Level.WARNING, logUnhandledExceptionMsg, LocalTime.now());
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
