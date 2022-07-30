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
    private ServerSocket serverSocket;
    private final int port;
    private Buffer buffer;
    //private UserConnectionCallback clientConnectionListener;
    private ExecutorService threadPool;

    private ArrayList<UserConnectionCallback> clientConnectionListenerList;
    private ArrayList<User> onlineClients;



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
     * Function for handling server exceptions,
     * reusable for every Runnable task the server calls which can t(ServerConnect, ServerDisconnect...)
     * @param e the exception to be handled
     * @param thread the thread in which the Exception occurred
     */
    private void handleServerException(IOException e, Thread thread){
        if(e instanceof SocketException){
            log.log(Level.SEVERE, e.getMessage());
        }
        else if(e instanceof EOFException){
            log.log(Level.WARNING, e.getMessage());
        }

    }

    /**
     * Remove User object from buffer
     * check size, if size = 0 don't fetch elements from buffer --> no clients to update + wait condition in buffer
     * if size >= 1, fetch all keys 'User' Object(s)
     * for each connected client (iterate over size) previously returned
     * write to data structure of user to ObjectOutputStream
     * @param disconnectedClient the client whose socket was closed.
     */
    @Override
    public void onUserDisconnectListener(User disconnectedClient) {
    }

    /**
     * Implementation of the UserConnectionCallback interface,
     * fires on successful client connection
     * @param connectedClient
     */
    @Override
    public void onUserConnectListener(User connectedClient) {
        System.out.println(connectedClient + " connected");
        if(buffer.size() >= 1){
            Set<User> set = buffer.getKeySet();

            // if list isn't null there is no need to instantiate
            // the already existing instance and fill it once again
            // simply remove the client and then execute the update function for all threads
            if(onlineClients != null){
                onlineClients.add(connectedClient);
                updateAllOnlineLists(set);
            }
            // if list of online clients is null, we need to create and populate the list
            if(onlineClients == null){
                System.out.println("online clients is null, instantiating list");
                onlineClients = new ArrayList<>();
                onlineClients.addAll(set);
                updateAllOnlineLists(set);
            }
        }
    }

    /**
     * after populating the list in one of the callback interface implementations above,
     * we send the list of connected clients to each connected client
     * this is achieved by iterating over the set of users passed into the func.
     * By invoking buffer.get(user) we can acquire a reference for each connected client
     * since buffer is a hashmap<K,V>, where K = user and V = socket.
     * @param set Set<User>, used for getting each value from the Buffer HashMap<User,Socket>
     */
    private void updateAllOnlineLists(Set<User> set){
        // Iterate over all users in Set fetched from buffer
        for (User user: set) {
            try{
                // For each user, acquire a reference to the socket.
                Socket clientSocket = buffer.get(user);

                // Log the event to track which clients we are updating in cli
                log.log(Level.INFO, LoggerUtil.ANSI_PURPLE + "Updating online list for "
                        + clientSocket.getRemoteSocketAddress().toString() + "\n" + LoggerUtil.ANSI_BLUE);

                // For each user, execute the SendObject runnable,
                // effectively updating each connected clients "currently online list"
                threadPool.execute(new SendObject(this.onlineClients, clientSocket));
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
        private Socket client;
        public SendObject(Message message, Socket client){
            this.message = message;
            this.client = client;
        }
        public SendObject(ArrayList<User> userArrayList, Socket client){
            this.userList = userArrayList;
            this.client = client;
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
                    handleServerException(e, Thread.currentThread());
                }
                e.printStackTrace();
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

                // While server is running,
                // 1 accept all incoming clients,
                //      For each client:
                //          read the User object from ObjectInputStream
                //          add Key-Value pair: <User,Socket> to buffer

                // in a real world application the hashmap would
                // allow for fast lookup for duplicate usernames
                // given that User is the key
                while(true){
                    Socket cSocket = serverSocket.accept();
                    InputStream is = cSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    User user = (User) ois.readObject();
                    buffer.put(user, cSocket);

                    for (UserConnectionCallback callbackImpl: clientConnectionListenerList) {
                        callbackImpl.onUserConnectListener(user);
                    }


                    //threadPool.execute(new ReceiveMessage(cSocket));
                    Thread.sleep(250);
                    //next client is processed,
                    // else thread just waits because serversocket.accept is a blocking operation
                }
            }
            catch (IOException e) {handleServerException(e, Thread.currentThread());}
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
