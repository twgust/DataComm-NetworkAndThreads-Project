package client.controller;

import entity.User;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
    // A list containing all currently online users
    private ArrayList<User> userOnlineList;

    // A list of the clients Contacts, each contact is a user object.
    // Loaded from local storage on startup, Saved and updated to local storage on exit
    private ArrayList<User> userContactList;

    private Socket clientSocket;
    private String ip;
    private int port;

    public ClientController(String ip, int port){
        this.ip = ip;
        this.port = port;

    }
    public ClientController(){

    }

    public void connectToServer(){
        ClientConnect connectThread = new ClientConnect();
        Thread t = new Thread(connectThread);
        t.start();
    }
    public void sendMessage(String message, ImageIcon icon){

    }
    public void sendMessage(String message){

    }
    public void sendMessage(ImageIcon icon){

    }
    private class ClientConnect implements Runnable{
        @Override
        public void run() {


            try {
                InetAddress address = InetAddress.getByName(ip);
                clientSocket = new Socket(address, port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
    private class ClientDisconnect implements Runnable{
        @Override
        public void run() {

        }
    }
}
