package server.controller;

import entity.Message;
import entity.User;
import entity.UserSet;
import server.Entity.Client;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.ServerInterface.LoggerCallBack;
import server.controller.ServerInterface.MessageReceivedListener;
import server.controller.ServerInterface.UserConnectionCallback;
import server.controller.Threads.ServerMainThread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;


/**
 * Notes and Requirements for Server:
 *
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 *
 * ----------------------------------------------------------------------------*
 * NonFunctional (qualitative) Requirements to implement for Server:
 *
 * 1, Handle a large amount of clients
 * ----------------------------------------------------------------------------*
 *
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Server:
 *
 * 1, Allow for a Client to connect to the server [X]
 * 2, Allow for a Client to disconnect from the server [X]
 *
 *
 * 3, Keep an updated List of currently connected Clients in some Data Structure [X]
 * 3.1 Operations on the Data Structure need be Synchronized [X]
 *
 *
 * 4, Store a Messages that can't be sent in some Data Structure []
 * 4.1 Operations on the Data Structure need be Synchronized []
 * 4.2 On Client reconnect, send the stored Message []
 *
 * 5, Log all traffic on the server to a locally stored File []
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) [x] Partial
 * ----------------------------------------------------------------------------*
 */

public class ServerController
        implements UserConnectionCallback, MessageReceivedListener {

    // server
    private final int port;
    private final ClientBuffer clientBuffer;
    private final UserBuffer userBuffer;

    // server threads
    private ExecutorService threadPool;
    private ExecutorService serverExecutor;

    private ServerMainThread serverMainThread;

    // when calling a function of the reference the implementations fire
    private LoggerCallBack loggerCallback;
    private UserConnectionCallback userConnectionCallback;
    private MessageReceivedListener messageReceivedListener;
    private ArrayList<MessageReceivedListener> messageReceivedListeners;
    /**
     * Constructor
     * @param port The port on which the Server is run on.
     */
    public ServerController(int port){
        clientBuffer = new ClientBuffer();
        userBuffer = new UserBuffer();
        this.port = port;
        startServer();
    }

    /**
     * List of implementations so ServerGUI and ServerController can provide their own impl.
     * @param connectionListener implementation of UserConnectionCallBack Interface
     */
    public void addConnectionListener(UserConnectionCallback connectionListener){
        this.userConnectionCallback = connectionListener;
    }
    public void addMessageReceivedListener(MessageReceivedListener listener){
       this.messageReceivedListener = listener;
    }
    public void addMessageReceivedListenerThread(MessageReceivedListener listener){
        if(messageReceivedListeners == null){
            messageReceivedListeners = new ArrayList<>();
        }
        messageReceivedListeners.add(listener);
    }
    public void addLoggerCallbackImpl(LoggerCallBack impl){
        this.loggerCallback = impl;
    }

    /**
     * should more callback listeners be implemented
     */
    private void registerCallbackListeners(){
        addConnectionListener(this);
        addMessageReceivedListener(this);
    }

    /**
     * Starts and initializes server,
     * invoked by ServerGUI.
     */
    public void startServer(){
        registerCallbackListeners();

        // log to server gui
        String logStartServerMsg = "initializing server on port: [" + port + "]";
        loggerCallback.logInfoToGui(Level.INFO, logStartServerMsg , LocalTime.now());

        // start Server
        serverExecutor = Executors.newSingleThreadExecutor();
        threadPool = Executors.newCachedThreadPool();
        serverMainThread = new ServerMainThread(port, clientBuffer, loggerCallback, userConnectionCallback);
        serverExecutor.execute(serverMainThread);
    }

    /**
     * Closes servers and all client connections,
     * invoked by ServerGUI.
     */
    public void stopServer(){
        ServerDisconnect disconnect = new ServerDisconnect();
    }

    /**
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
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
            for (User u: clientBuffer.getKeySet()) {
                try{
                    // for each user in buffer, update their "online list"
                    // Runnable still runs on thread assigned at start of func pool.
                    new SendObject(new UserSet(userHashSet,connectedClient),u,
                            clientBuffer.get(u).getSocket(),
                            clientBuffer.get(u).getOos()).run();
                }catch (InterruptedException e){e.printStackTrace();}
            }
        }));
    }
    /**
     * WIP
     * Removes disconnected K:User V: Socket from buffer,
     * Removes User from onlineList
     * write to data structure of user to ObjectOutputStream
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
        System.out.println("ok!");
        try{
            clientBuffer.removeUser(disconnectedClient);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        Set<User> set = clientBuffer.getKeySet();
        // updateOnlineUsersForClients(); TODO

        // log to server gui
        String logDisconnectMessage = disconnectedClient + " disconnected fro server";
        loggerCallback.logInfoToGui(Level.INFO,logDisconnectMessage, LocalTime.now());
    }

    /**
     *
     * @param message
     * @param client
     * @param oos
     */
    @Override
    public void onMessageReceived(Message message, Socket client, ObjectOutputStream oos) {
        threadPool.execute(()->{
            loggerCallback.logInfoToGui(Level.INFO, "message received from: "
                    + message.getAuthor()+ " [" + message.getTextMessage()+"]", LocalTime.now());

            for (User user: message.getRecipientList()) {

                new SendObject(message,client,oos).run();
            }
        });


    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendObject implements Runnable{
        // update lists
        private UserSet objectOutSet;
        private User user; // recipient of message

        //send message
        private Message message;

        // socket and stream to send to
        private Socket socket;
        private ObjectOutputStream oos;



        /**
         * Constructor for sending messages to clients
         * @param message message to be sent
         * @param socket client which sent the request to server
         */
        public SendObject(Message message, Socket socket, ObjectOutputStream oos){
            this.message = message;
            this.socket = socket;
            this.oos = oos;
        }

        /**
         * Updates list of currently online users
         * @param userSet the set of users + the recent disconnect/connected user as an obj
         * @param user the user of the user which the update is going to be sent to,
         */
        public SendObject(UserSet userSet, User user, Socket socket, ObjectOutputStream oos){
            this.socket = socket;
            this.oos =oos;
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
                if(this.objectOutSet != null){
                    String str = "Sending UserSet:{'<Hashset<User>', User: '"+ objectOutSet.getHandledUser() + "'} " +
                            "to --> " +  user + " @"  + socket.getInetAddress();
                    loggerCallback.logInfoToGui(Level.INFO, str, LocalTime.now());

                    oos.writeObject(objectOutSet);
                    oos.reset();
                }
                /**
                 * TODO
                 */
                else if(this.message != null){

                }



            } catch (IOException e) {
                if(e instanceof SocketException){
                    e.printStackTrace();
                    handleIOException(e,"SendObject.run()", Thread.currentThread(),user,"");
                }
            }
        }
    }
    /**
     * Runnable for receiving a message from a client
     * Reads from connected clients ObjectInputStream.
     */
    private class ReceiveMessage implements Runnable{
        private Client client;
        public ReceiveMessage(Client client){
            this.client = client;
        }
        @Override
        public void run() {
            while(true){
                try{
                    ObjectInputStream ois = client.getOis();
                    Object o = ois.readObject();
                    if(o instanceof Message){
                        Message message = (Message)o;
                        messageReceivedListener.onMessageReceived(message, client.getSocket(), client.getOos());
                    }

                    Thread.sleep(250);
                }
                catch (InterruptedException e){
                    e.printStackTrace();}
                catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * Runnable for closing the server
     */
    private class ServerDisconnect implements Runnable{
        @Override
        public void run() {

        }
    }
    /**
     * WIP
     * Function for handling Socket exceptions
     * @param e the exception to be handled
     * @param thread the thread in which the Exception occurred
     */
    private void handleIOException(IOException e, String methodName, Thread thread, User user, String clientIP){
        if(clientIP.isEmpty()){
            clientIP = "exception occurred in unknown client";
        }
        if(e instanceof SocketException){
            // assume user disconnect, at any rate the SocketException has the effect of a disconnected user

            // fire the onUserDisconnect event for each implementation of the interface (Server Controller,GUI)
            userConnectionCallback.onUserDisconnectListener(user);

            // log to server gui
            String logSocketExceptionMsg = "Client IP: [" + clientIP + "]  USER:" + user + " disconnected";
            loggerCallback.logInfoToGui(Level.WARNING,logSocketExceptionMsg, LocalTime.now());
        }
        else if(e instanceof EOFException){
            // log to server gui
            String logEndOfFileMsg = "Client: EOF exception for " + user.getUsername() + " in " + methodName;
            loggerCallback.logInfoToGui(Level.WARNING, logEndOfFileMsg, LocalTime.now());
        }
        else{
            // log to server gui
            String logUnhandledExceptionMsg = "UNHANDLED EXCEPTION\n" + e.getMessage();
            loggerCallback.logInfoToGui(Level.WARNING, logUnhandledExceptionMsg, LocalTime.now());
        }
    }
    private void handleInterruptedException(Exception e){

    }
}
