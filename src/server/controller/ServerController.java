package server.controller;

import entity.Message;
import entity.User;
import entity.LoggerUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


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
 * 1, Allow for a Client to connect to the server []
 * 2, Allow for a Client to disconnect from the server []
 *
 *
 * 3, Keep an updated List of currently connected Clients in some Data Structure []
 * 3.1 Operations on the Data Structure need be Synchronized
 *
 *
 * 4, Store a Messages that can't be sent in some Data Structure []
 * 4.1 Operations on the Data Structure need be Syncrhonized
 * 4.2 On Client reconnect, send the stored Message []
 *
 * 5, Log all traffic on the server to a locally stored File []
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) []
 * ----------------------------------------------------------------------------*
 */

public class ServerController implements UserConnectionCallback {
    private final Logger log;

    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    private final int port;
    private final Buffer buffer;

    private ArrayList<UserConnectionCallback> clientConnectionListenerList;
    private ArrayList<User> onlineUserList;

    /**
     * Constructor
     * @param port The port on which the Server is run on.
     */
    public ServerController(int port){
        log = Logger.getLogger("Server");
        threadPool = Executors.newCachedThreadPool();
        addConnectionListener(this);
        buffer = new Buffer();
        this.port = port;
    }

    /**
     * List of implementations so ServerGUI and ServerController can provide their own impl.
     * @param connectionListener implementation of UserConnectionCallBack Interface
     */
    public void addConnectionListener(UserConnectionCallback connectionListener){
        if(clientConnectionListenerList == null){
            clientConnectionListenerList = new ArrayList<>();
        }
        clientConnectionListenerList.add(connectionListener);
    }

    /**
     * Starts and initializes server,
     * invoked by ServerGUI.
     */
    public void startServer(){
        ServerConnect connect = new ServerConnect();
        Thread T = new Thread(connect);
        T.start();
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
     * Function for handling Socket exceptions
     * @param e the exception to be handled
     * @param thread the thread in which the Exception occurred
     */
    private void handleServerException(IOException e,Thread thread,User user, String clientIP){
        if(clientIP.isEmpty()){
            clientIP = "unknown client";
        }
        if(e instanceof SocketException){
            // fire the onUserDisconnect event for each implementation of the interface (Server Controller,GUI)
            for (UserConnectionCallback impl: clientConnectionListenerList) {
                impl.onUserDisconnectListener(user);
            }
            log.log(Level.INFO, LoggerUtil.ANSI_PURPLE + "Client: " + clientIP + " disconnected" + LoggerUtil.ANSI_BLUE);

        }
        else if(e instanceof EOFException){
            log.log(Level.WARNING, e.getMessage());
        }
    }

    /**
     * Removes disconnected K:User V: Socket from buffer,
     * Removes User from onlineList
     * write to data structure of user to ObjectOutputStream
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
        try{
            buffer.removeUser(disconnectedClient);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        onlineUserList.remove(disconnectedClient);
        Set<User> set = buffer.getKeySet();
        updateAllOnlineLists(set);
    }

    /**
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * @param connectedClient
     */
    @Override
    public void onUserConnectListener(User connectedClient) {
        System.out.println(connectedClient + " connected");
            Set<User> set = buffer.getKeySet();

            // if list isn't null there is no need to instantiate
            // the already existing instance and fill it once again
            // simply add the client.
            if(onlineUserList != null){
                onlineUserList.add(connectedClient);
                updateAllOnlineLists(set);
            }
            // if list of online clients is null, we need to create and populate the list
            if(onlineUserList == null){
                System.out.println("online clients is null, instantiating list");
                onlineUserList = new ArrayList<>();
                onlineUserList.addAll(set);
                updateAllOnlineLists(set);
            }
        }


    /*
     * Invoked by one of the connection callbacks (onUserConnect/onUserDisconnect)
     * Iterates over all connected sockets, each socket is sent the updated onlineUserList
     * @param set Set of Keys to enable access operation on the Buffer HashMap (K:User, V:Socket)
     */
    private void updateAllOnlineLists(Set<User> set){
        // Iterate over all users in Set fetched from buffer to get a reference to socket
        for (User user: set) {
            try{
                Socket clientSocket = buffer.get(user);
                String clienIP = clientSocket.getRemoteSocketAddress().toString();
                log.log(Level.INFO, LoggerUtil.ANSI_PURPLE + "Updating online list for "
                        + clientSocket.getRemoteSocketAddress().toString() + "\n" + LoggerUtil.ANSI_BLUE);
                // For each user, execute the SendObject runnable, updating each clients OnlineList
                SendObject sendObject = new SendObject(this.onlineUserList, user, clientSocket, clienIP);
                threadPool.execute(sendObject);
            }catch (InterruptedException e){e.printStackTrace();}
        }
    }
    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendObject implements Runnable{
        private ArrayList<User> userList;
        private Message message;
        private User user;
        private Socket client;
        private String clientIP;
        public SendObject(Message message, Socket client){
            this.message = message;
            this.client = client;
        }
        public SendObject(ArrayList<User> userArrayList, User user, Socket client, String ip){
            this.userList = userArrayList;
            this.client = client;
            this.clientIP = ip;
            this.user = user;
        }

        @Override
        public void run() {
            try {
                OutputStream os = client.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                if(userList != null){
                    ArrayList<User> temp = userList;
                    oos.writeObject(temp);
                    oos.flush();
                }
                else if(message != null){
                    oos.writeObject(message);
                    oos.flush();
                }

            } catch (IOException e) {
                if(e instanceof SocketException){
                    handleServerException(e, Thread.currentThread(),user,  clientIP);
                }
            }
        }
    }
    /**
     * Runnable for receiving a message from a client
     * Reads from connected clients ObjectInputStream.
     */
    private class ReceiveMessage implements Runnable{
        private Socket socket;
        public ReceiveMessage(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            while(true){
                try{
                    System.out.println("receiving message from client " +
                            "<" + socket.getRemoteSocketAddress() + "> " +
                            "sleeping for 5000");
                    Thread.sleep(250);
                }
                catch (InterruptedException e){e.printStackTrace();}
            }
        }
    }

    /**
     * Runnable for initializing the server
     */
    private class ServerConnect implements Runnable{
        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket(port);
                log.log(Level.INFO,  LoggerUtil.ANSI_GREEN + " Server running...\n" + LoggerUtil.ANSI_BLUE);
                while(true){
                    Socket cSocket = serverSocket.accept();
                    InputStream is = cSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    User user = (User) ois.readObject();
                    buffer.put(user, cSocket);

                    // List of implementations, so both ServerController and ServerGUI and
                    // can provide their own implementations of the interface.
                    for (UserConnectionCallback callbackImpl: clientConnectionListenerList) {
                        callbackImpl.onUserConnectListener(user);
                    }


                    Thread.sleep(250);
                }
            }
            catch (IOException e) {handleServerException(e,Thread.currentThread(),null, "");}
            catch (InterruptedException e) {e.printStackTrace();}
            catch (ClassNotFoundException e) {e.printStackTrace();}
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
}
