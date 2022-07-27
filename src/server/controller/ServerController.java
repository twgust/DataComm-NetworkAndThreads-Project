package server.controller;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.SocketException;


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
    private static final String serverStr = "Server: ";

    private ServerSocket serverSocket;
    private final int port;

    /**
     * Constructor
     * @param port The port on which the Server is run on.
     */
    public ServerController(int port){
        this.port = port;
    }

    /**
     * Starts and initializes server,
     * invoked by ServerGUI.
     */
    public void startServer(){
        ServerConnect connect = new ServerConnect();
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
            System.out.println(serverStr + "socket exception on " + thread.getName());
        }
        else if(e instanceof EOFException){
            System.out.println("eof on " + thread.getName());
        }
    }

    /**
     * Runnable for receiving a message from a client
     * Reads from connected clients ObjectInputStream.
     */
    private class ReceiveMessage implements Runnable{
        @Override
        public void run() {
            // ObjectInputStream reads Message from client

        }
    }

    /**
     * Runnable for sending a message to a Client,
     * Writes to connected clients ObjectOutputStream
     */
    private class SendMessage implements Runnable{
        @Override
        public void run() {
            // ObjectOutputStream writes Message to List of recipients by invoking message.getRecipientList()

        }
    }

    /**
     * Runnable for initializing the server
     */
    private class ServerConnect implements Runnable{
        @Override
        public void run(){
            try {
                serverSocket = new ServerSocket();
            }
            catch (IOException e) {
                handleServerException(e, Thread.currentThread());}
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
