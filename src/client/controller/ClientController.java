package client.controller;

import entity.Message;
import entity.User;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Notes and Requirements for Client:
 *
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 *
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Client:
 *
 * 1, Connect to server: []
 * 2, Create a username and profile picture before connecting: []
 * 3, Disconnect from server: []
 *
 * 3, Send messages to User(s) - through Server: []
 * 4, Receive messages from User(s) - through Server: []
 *
 * 5, Display connected User(s): []
 * 6, Add connected users to a Contact list: []
 * 7, Save contact list on local storage ON client disconnect and exit:  []
 * 8, Load contact list from local storage On client connect and startup: []
 * 9, Select recipients of message from Contact list: []
 * 10, Select recipients of message from Online list: []
 *
 * All funcitionality described above - with exception to 7 & 8 -
 * should be available through the ClientView (GUI): []
 * ----------------------------------------------------------------------------*
 */

public class ClientController {
    private Logger log;

    // A list containing all currently online users
    private ArrayList<User> userOnlineList;

    // A list of the clients Contacts, each contact is a user object.
    // Loaded from local storage on startup, Saved and updated to local storage on exit
    private ArrayList<User> userContactList;

    private Socket clientSocket;
    private String ip;
    private int port;

    private InputStream is;
    private ObjectInputStream ois;

    private OutputStream os;
    private ObjectOutputStream oos;

    private ExecutorService executorService;
    private ReceiveMessage receiveMessage;
    private SendMessage sendMessage;
    private User user;

    public ClientController(String ip, int port){
        executorService = Executors.newSingleThreadExecutor();
        log = Logger.getLogger("client");
        this.port = port;
        this.ip = ip;
    }
    public ClientController(){

    }
    public void createUser(String name){

    }

    /**
     * If a client hasn't been connected to the server before and has no username
     *
     * @param username
     */
    public void registerUser(String username){

    }

    public void connectToServer(String username){
        //executorService.execute(new ClientConnect(username));
        ClientConnect connect = new ClientConnect(username);
        Future<?> connectTask = executorService.submit(connect);
        while(true){
            if (connectTask.isDone()){
                break;
            }
        }
        executorService.execute(new ReceiveMessage());

    }
    public void disconnectFromServer(){
        ClientDisconnect disconnectThread = new ClientDisconnect();
        Thread t = new Thread(disconnectThread);
        try{
            t.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * method overloading for send chat msg functions,
     * since a Message can contain
     * Text OR Image OR Text AND Image
     * @param message String, any alphanumeric + special character
     * @param icon Image, jpg or png
     */
    public void sendChatMsg(String message, ImageIcon icon){

    }
    public void sendChatMsg(String message){

    }
    public void sendChatMsg(ImageIcon icon){

    }

    /**
     * *** WIP ***
     */
    private class ReceiveMessage implements Runnable{
        @Override
        public void run() {
            while(!clientSocket.isClosed()){
                    try{
                        is = clientSocket.getInputStream();
                        ois = new ObjectInputStream(is);
                        Object o = ois.readObject();
                        serverResponseHandler(o);

                        Thread.sleep(5000);
                    }
                    catch (Exception e) {e.printStackTrace();}
            }
        }
    }

    /**
     * *** WIP ****
     * ineffective solution
     * @param o Object read from ObjectInputStream
     */
    private void serverResponseHandler(Object o){
        if(o instanceof List){
            for (Object element: (List<?>)o) {
                if(element instanceof User){
                    // o =  List<User>
                }
            }
        }
        else if (o instanceof Message){
            // o = Message
        }
    }

    private class SendMessage implements Runnable{
        public SendMessage(Message message){

        }
        @Override
        public void run() {

        }
    }

    private class ClientConnect implements Runnable{
        /**
         * Default constructor used for unregistered clients
         */
        private String username;
        public ClientConnect(String username){
            this.username = username;
        }

        @Override
        public void run() {
            try {
                InetAddress address = InetAddress.getByName(ip);
                clientSocket = new Socket(address, port);
                os = clientSocket.getOutputStream();
                oos = new ObjectOutputStream(os);
                oos.writeObject(new User(this.username));
                os.flush();
                oos.flush();
                // start listening to messages from server
                // after connection has been established
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }

    private class ClientDisconnect implements Runnable{
        @Override
        public void run() {
            try{
                clientSocket.close();
            }
            catch (NullPointerException e ){
                log.log(Level.WARNING, "socket not initialized");
                e.printStackTrace();

            } catch (IOException e) {
                log.log(Level.WARNING, " exception in closing streams");
                e.printStackTrace();
            }
        }
    }
}
