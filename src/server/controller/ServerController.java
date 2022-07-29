package server.controller;

import entity.User;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

public class ServerController {
    private final Logger log;

    private ServerSocket serverSocket;
    private final int port;
    private Buffer buffer;
    private UserConnectionCallback userConnection;
    private ExecutorService threadPool;



    /**
     * Constructor
     * @param port The port on which the Server is run on.
     */
    public ServerController(int port){
        log = Logger.getLogger("Server");
        threadPool = Executors.newCachedThreadPool();

        buffer = new Buffer();
        this.port = port;
    }
    public void addConnectionListener(UserConnectionCallback connection){
        this.userConnection = connection;
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
                            "sleeping for 3.5s");
                    Thread.sleep(3500);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendMessage implements Runnable{
        @Override
        public void run() {

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
                int i = 1;
                // While server is running, accept all incoming clients, read their username
                // add Key-Value pair: <User,Socket> to buffer
                // in a real world application the hashmap would allow for fast lookup for duplicate usernames
                // given that User is the key
                while(true){
                    Socket cSocket = serverSocket.accept();
                    InputStream is = cSocket.getInputStream();
                    DataInputStream dis = new DataInputStream(is);
                    String username = dis.readUTF();
                    User user = new User(username);
                    buffer.put(user, cSocket);
                    userConnection.onUserConnectListener(user);

                    threadPool.execute(new ReceiveMessage(cSocket));
                    Thread.sleep(250);
                    //next client is processed,
                    // else thread just waits because serversocket.accept is a blocking operation
                }
            }
            catch (IOException e) {handleServerException(e, Thread.currentThread());}
            catch (InterruptedException e) {e.printStackTrace();}
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
