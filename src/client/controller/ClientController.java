package client.controller;

import entity.Message;
import entity.User;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        log = Logger.getLogger("client");
        executorService = Executors.newSingleThreadExecutor();
        this.port = port;
        this.ip = ip;
    }
    public ClientController(){

    }
    public void registerUser(String username){
        executorService.execute(()  ->{
            try{
                OutputStream oos = clientSocket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(oos);
                dos.writeUTF(username);
                dos.flush();
            }catch (Exception e){
                e.printStackTrace();
            }

        });
    }

    public void connectToServer(){
        ClientConnect connectThread = new ClientConnect();
        Thread t = new Thread(connectThread);
        System.out.println("starting thread " + t.getName());
        t.start();
        try{
            t.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }

    }
    public void sendChatMsg(String message, ImageIcon icon){

    }
    public void sendChatMsg(String message){

    }
    public void sendChatMsg(ImageIcon icon){

    }
    private class ReceiveMessage implements Runnable{
        @Override
        public void run() {
            while(true){

            }
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
        public ClientConnect(){

        }

        @Override
        public void run() {
            try {
                InetAddress address = InetAddress.getByName(ip);
                clientSocket = new Socket(address, port);
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }
    private class ClientDisconnect implements Runnable{
        @Override
        public void run() {
            try{
                if(!clientSocket.isInputShutdown()){
                    // close input streams
                    ois.close();
                    is.close();
                }
                if(!clientSocket.isOutputShutdown()){
                    // shutdown output streams
                    os.close();
                    oos.close();
                }
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
