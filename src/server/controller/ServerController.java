package server.controller;

import entity.Message;
import entity.User;
import entity.UserSet;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
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
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) [] Partial
 * ----------------------------------------------------------------------------*
 */

public class ServerController
        implements UserConnectionCallback, MessageReceivedListener {
    Class serverClass = ServerController.class;

    private LocalTime time;

    private ServerSocket serverSocket;
    private final ExecutorService threadPool;

    // server
    private final int port;
    private final ClientBuffer clientBuffer;
    private UserSet userSet;

    // when calling a function of the reference the implementations fire
    private LoggerCallBack loggerCallback;
    private UserConnectionCallback userConnectionCallback;
    private MessageReceivedListener messageReceivedListener;
    private ArrayList<MessageReceivedListener> messageReceivedListeners;
    private HashSet<User> hashSet;
    /**
     * Constructor
     * @param port The port on which the Server is run on.
     */
    public ServerController(int port){
        threadPool = Executors.newCachedThreadPool();
        clientBuffer = new ClientBuffer();
        this.port = port;
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


        // log to server gui
        String logStartServerMsg = "initializing server on port: [" + port + "]";
        loggerCallback.logInfoToGui(Level.INFO, logStartServerMsg , LocalTime.now());

        // start server thread
        ServerConnect connect = new ServerConnect(this);
        Thread T = new Thread(connect);
        T.start();
        registerCallbackListeners();
    }

    /**
     * Closes servers and all client connections,
     * invoked by ServerGUI.
     */
    public void stopServer(){
        ServerDisconnect disconnect = new ServerDisconnect();
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
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * @param connectedClient connected Client
     */
    @Override
    public void onUserConnectListener(User connectedClient) {
        // get the KeySet (set of users) from buffer that has an active connection with server
        Set<User> bufferKeySet = clientBuffer.getKeySet();
        UserSet set;

        // default case
        if (hashSet!=null){
            hashSet.add(connectedClient);
            set = new UserSet(hashSet, connectedClient);
            updateOnlineUsersForClients(bufferKeySet,set);
        }

        // only one user online, why display yourself?
        if (hashSet == null) {
            hashSet = new HashSet<>();
            bufferKeySet.parallelStream().forEach((u) -> {
                hashSet.add(u);
            });
            set = new UserSet(hashSet, connectedClient);
        }

        String logOnUserConnectMsg;
        logOnUserConnectMsg = "adding " + connectedClient + " to list of online clients";
        loggerCallback.logInfoToGui(Level.INFO, logOnUserConnectMsg, LocalTime.now());
    }
    @Override
    public void onMessageReceived(Message message, Socket client, ObjectOutputStream oos) {
        loggerCallback.logInfoToGui(Level.INFO, "message received from: "
                + message.getAuthor()+ " [" + message.getTextMessage()+"]", LocalTime.now());
        for (User user: message.getRecipientList()) {
            try{
                threadPool.execute(new SendObject(message, clientBuffer.get(user)));
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    /*
     * Invoked by one of the connection callbacks (onUserConnect/onUserDisconnect)
     * Iterates over all connected sockets, each socket is sent an updated HashSet and handled user (UserSet)
     * @param set of Keys to enable access operation on the Buffer HashMap (K:User, V:Socket)
     */
    private synchronized void updateOnlineUsersForClients(Set<User> set, UserSet objectOutUserSet){
        // Iterate over the set of connected Users
        for (User user: set) {
            try{
                // for each online user: grab a reference to their Socket
                Client client = clientBuffer.get(user);

                // log to server gui
                String logUpdateOnlineListMsg = "Updating online list for " + user + " - " + set.size();
                loggerCallback.logInfoToGui(Level.INFO, logUpdateOnlineListMsg, LocalTime.now());
                System.out.println(objectOutUserSet.getUserSet().size() + " ? for " + user);
                // For each user: assign a thread from pool which executes the SendObject runnable to the associated socket
                // effectively updating the client collection of online users
                SendObject updateListForClient = new SendObject(objectOutUserSet, user, client.getSocket(), client.getOos());
                threadPool.execute(updateListForClient);
            } catch (InterruptedException e){e.printStackTrace();}
        }
    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendObject implements Runnable{
        private UserSet objectOutSet;
        private User user; // recipient of message
        private Socket socket;
        private ObjectOutputStream oos;

        private Client client;
        private Message message;

        /**
         * Constructor for sending messages to clients
         * @param message message to be sent
         * @param client client which sent the request to server
         */
        public SendObject(Message message, Client client){
            this.message = message;
            this.client = client;
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
                if(this.objectOutSet != null){
                    oos.writeObject(objectOutSet);
                    System.out.println("wrote " + objectOutSet.getUserSet().size() + " to " + socket.getLocalPort());
                    oos.reset();
                }

                else if(this.message != null){
                    /* TODO
                    ObjectOutputStream oos  = client.getOos();
                    oos.writeObject(message);
                    oos.flush();
                    loggerCallback.logInfoToGui(Level.INFO, "writing "    +
                            message.getAuthor()+ " [" + message.getTextMessage()+"]", LocalTime.now());

                     */
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
                        System.out.println(message.getTextMessage());
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
     * Runnable for initializing the server
     */
    private class ServerConnect implements Runnable{
        private ServerController controller;
        public ServerConnect(ServerController controller){
            this.controller=controller;
        }
        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket(port);
                String logServerRunningMsg = " server running on port: [" + port + "]";
                loggerCallback.logInfoToGui(Level.INFO, logServerRunningMsg, LocalTime.now());

                // multithreaded server
                while(true){
                    Socket clientSocket = serverSocket.accept();
                    // start timer after client connection accepted
                    long start = System.currentTimeMillis();

                    // set up streams for client
                    InputStream is = clientSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    OutputStream os = clientSocket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);

                    // black magic wizardry
                    User user = null;
                    Object oUser = null;

                    // Step 1) read object
                    try{
                         oUser = ois.readObject();
                    }catch (EOFException e){
                        handleIOException(e,"ServerConnect.run()", Thread.currentThread(), null, "");
                    }
                    // Step 2) check type of generic object oUser since the client can other objects through the stream
                    if(oUser instanceof User){
                        // Step 3) cast the generic to User and read the byte[]
                        user = (User) oUser;
                        byte[] imgInBytes = user.getAvatarAsByteBuffer();

                        // Step 4) buffer the img and write it to server storage
                        ByteArrayInputStream bais = new ByteArrayInputStream(imgInBytes);
                        BufferedImage img = ImageIO.read(bais);
                        String path = "src/server/avatars/"+ user.getUsername() + ".jpg";
                        ImageIO.write(img, "jpg", new File(path)); //Save the file, works!
                        // ImageIcon imageIcon = new ImageIcon(imgInBytes); works!

                        // Step 5) put the buffer in the user, enabling server to perform operations on client
                        Client client = new Client(clientSocket, oos, ois);
                        clientBuffer.put(user, client);

                        // start a separate thread which listens to client
                        try{
                            threadPool.execute(new ReceiveMessage(clientBuffer.get(user)));

                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }

                    // log to server gui
                    long end = (System.currentTimeMillis() - start);
                    String logClientTimeToConnectMsg = "finished processing client [" + clientSocket.getLocalAddress() + "] in " + end + "ms";
                    loggerCallback.logInfoToGui(Level.INFO, logClientTimeToConnectMsg, LocalTime.now());

                    // log to server gui
                    String logUserConnectedMsg = user.getUsername() + " connected to server";
                    loggerCallback.logInfoToGui(Level.INFO, logUserConnectedMsg, LocalTime.now());

                    // step 6) fire implementation of userConnectionCallback
                    userConnectionCallback.onUserConnectListener(user);

                    System.out.println("good!");
                }
            }
            catch (IOException e) {handleIOException(e,"ServerConnect.run()",Thread.currentThread(),null, "");}
            catch (ClassNotFoundException e) {e.printStackTrace();}
        }
    }
    private void validateConnectionRequest(Socket client, Object o){

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
