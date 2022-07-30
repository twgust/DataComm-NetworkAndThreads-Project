package client.controller;

import entity.Message;
import entity.MessageType;
import entity.User;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notes and Requirements for Client:
 * <p>
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 * <p>
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Client:
 * <p>
 * 1, Connect to server: []
 * 2, Create a username and profile picture before connecting: []
 * 3, Disconnect from server: []
 * <p>
 * 3, Send messages to User(s) - through Server: []
 * 4, Receive messages from User(s) - through Server: []
 * <p>
 * 5, Display connected User(s): []
 * 6, Add connected users to a Contact list: []
 * 7, Save contact list on local storage ON client disconnect and exit:  []
 * 8, Load contact list from local storage On client connect and startup: []
 * 9, Select recipients of message from Contact list: []
 * 10, Select recipients of message from Online list: []
 * <p>
 * All funcitionality described above - with exception to 7 & 8 -
 * should be available through the ClientView (GUI): []
 * ----------------------------------------------------------------------------*
 */

// worth considering that 2N threads will occupy N cores.
public class ClientController {
    private Logger log;

    // A list containing all currently online users
    private IUserConnectionCallback connectedUsersCallback;
    private ArrayList<User> userOnlineList;

    // A list of the clients Contacts, each contact is a user object.
    // Loaded from local storage on startup, Saved and updated to local storage on exit
    private ArrayList<User> userContactList;

    private Socket clientSocket;

    private String ip;
    private int port;
    private int localPort;

    private InputStream is;
    private ObjectInputStream ois;

    private OutputStream os;
    private ObjectOutputStream oos;

    private final ExecutorService connectAndReceiveExecutor;
    private final ExecutorService sendMessageExecutor;

    private ReceiveMessage receiveMessage;
    private SendMessage sendMessage;
    private User user;

    public ClientController(String ip, int port) {
        log = Logger.getLogger("client");
        this.port = port;
        this.ip = ip;

        connectAndReceiveExecutor = Executors.newSingleThreadExecutor();
        sendMessageExecutor = Executors.newSingleThreadExecutor();
        userOnlineList = new ArrayList<>();
    }
    public void addCallBackListener(IUserConnectionCallback impl){
        this.connectedUsersCallback = impl;
    }

    /**
     * If a client hasn't been connected to the server before and has no username
     * @param username input from gui
     */
    public void registerUser(String username) {

    }

    /**
     * TODO add image icon as second input parameter
     * @param username user
     */
    public void connectToServer(String username) {
        // empty as of now
        ImageIcon icon = new ImageIcon();
        ClientConnect connect = new ClientConnect(username, icon);
        Future<?> connectTask = connectAndReceiveExecutor.submit(connect);
        while (true) {
            if (connectTask.isDone()) {
                break;
            }
        }
        connectAndReceiveExecutor.execute(new ReceiveMessage());
    }

    public void disconnectFromServer() {
    }

    /**
     * method overloading for send chat msg functions,
     * since a Message can contain
     * Text OR Image OR Text AND Image
     *
     * @param message String, any alphanumeric + special character
     * @param icon    Image, jpg or png
     */
    public void sendChatMsg(String message, ImageIcon icon, ArrayList<User> recipients) {
        // consider storing messages in some data struct.
        // user == client == author of message.
        if(user != null){
            Message imageTextMsg = new Message(message, icon, user, recipients, MessageType.TEXT_IMAGE);

            //todo will probably implement future object,
            // so a client can't send another message before the first
            // has successfully been written to ObjectOutputStream
            sendMessageExecutor.execute(new SendMessage(imageTextMsg));
        }
    }

    public void sendChatMsg(String message) {

    }

    public void sendChatMsg(ImageIcon icon) {

    }

    /**
     * Runnable for reading messages from server.
     *
     * Assignment states that all communication is to be done through Object IO Streams
     * would be way easier to handle responses for both client and server if communication
     * was done through json formatted strings.
     * E.g {"type": "message"
     * "text": "textValue":
     * "image": "imageValue" }
     * Just parse response by checking type
     * then switch statement for different types.
     */
    private class ReceiveMessage implements Runnable {
        @Override
        public void run() {
            while (!clientSocket.isClosed()) {
                try {
                    is = clientSocket.getInputStream();
                    ois = new ObjectInputStream(is);
                    Object o = ois.readObject();
                    if (o instanceof List<?>) {
                        handleListResponse(o, userOnlineList);

                        // call the interface after response has been handled
                        // --> gui updates
                        connectedUsersCallback.usersUpdated(userOnlineList);
                    }
                    else if (o instanceof Message) { handleMessageResponse(o); }
                    Thread.sleep(250);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e instanceof EOFException){
                        System.out.println("end of file, going to sleep for a sec");
                        try {Thread.sleep(1000);}
                        catch (InterruptedException ex) {e.printStackTrace();}
                    }
                }
            }
        }
    }

    /**
     * Distinguishing two generic objects by their
     * generic parameter is simply not something you can do in Java,
     * so Receive message has to invoke this mess of a solution.
     * @param o Object read from ObjectInputStream
     */
    private synchronized void handleListResponse(Object o, ArrayList<User> list) {
        list.clear();
        System.out.println("\nCLIENT");
        for (Object element : (List<?>) o) {
            if (element instanceof User) {
               list.add((User)element);
                System.out.println(element.toString() + "added to list in client " + clientSocket.getLocalPort());
            }
            // here we can add functionality for other List<?> responses
        }
    }

    private synchronized void handleMessageResponse(Object o) {
        Message message = (Message) o;
        switch (message.getType()) {
            case TEXT -> System.out.println("type: text");
            case IMAGE -> System.out.println("type: image");
            case TEXT_IMAGE -> System.out.println("type: textimage");
        }
    }

    private class SendMessage implements Runnable {
        public SendMessage(Message message) {}
        @Override
        public void run() {}
    }

    private class ClientConnect implements Runnable {
        /**
         * Default constructor used for unregistered clients
         */
        private String username;
        private ImageIcon icon;

        public ClientConnect(String username, ImageIcon icon) {
            this.username = username;
            this.icon = icon;
            //TODO implement new User(String username, ImageIcon)
            user = new User(username);
        }

        @Override
        public void run() {
            try {
                assert user != null;

                InetAddress address = InetAddress.getByName(ip);
                clientSocket = new Socket(address, port);
                os = clientSocket.getOutputStream();
                oos = new ObjectOutputStream(os);
                oos.writeObject(user);
                os.flush();
                oos.flush();
                // end of runnable
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientDisconnect implements Runnable {
        @Override
        public void run() {
            try {
                clientSocket.close();
            } catch (NullPointerException e) {
                log.log(Level.WARNING, "socket not initialized");
                e.printStackTrace();

            } catch (IOException e) {
                log.log(Level.WARNING, " exception in closing streams");
                e.printStackTrace();
            }
        }
    }
    public String getServerIP() {
        return ip;
    }

    public int getServerPort() {
        return port;
    }
    public int getLocalPort() {
        if(clientSocket != null){
            return clientSocket.getLocalPort();
        }
        return -1;
    }
}
