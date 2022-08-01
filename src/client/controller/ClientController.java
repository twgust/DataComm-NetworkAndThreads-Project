package client.controller;

import entity.Message;
import entity.MessageType;
import entity.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * 5, Display connected User(s): [X]
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

/**
 *
 */
public class ClientController {
    private Logger log;

    private IConnectionHandler connectionHandler;
    private IMessageReceivedHandler msgReceivedHandler;

    // A list containing all currently online users
    private ArrayList<User> userOnlineList;

    // TODO undecided on solution and data struct A Set of the clients Contacts, E = user
    // Loaded from local storage on startup, Saved and updated to local storage on exit
    private HashSet<User> userContactList;

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

    /**
     *
     * @param ip ip or server
     * @param port port of server
     */
    public ClientController(String ip, int port) {
        log = Logger.getLogger("client");
        this.port = port;
        this.ip = ip;

        // since connection is only done once, we might as well reuse that single Thread.
        connectAndReceiveExecutor = Executors.newSingleThreadExecutor();
        sendMessageExecutor = Executors.newSingleThreadExecutor();
        userOnlineList = new ArrayList<>();
        userContactList = new HashSet<>();
    }

    /**
     * @param impl of interface from ClientGUI
     */
    public void addConnectionHandler(IConnectionHandler impl) {
        this.connectionHandler = impl;
    }
    /**
     * @param impl of interface from ClientGUI
     */
    public void addMessageReceivedHandler(IMessageReceivedHandler impl){
        this.msgReceivedHandler = impl;
    }

    /**
     * Loads image from path
     * @param path path of file
     */
    public  ImageIcon loadImgFromPath(String path){
        return new ImageIcon("path");
    }

    /**
     * TODO, the idea is that we want to be able to distinguish between already registered users and new users
     * @param username input from gui
     */
    public void registerUser(String username) {

    }

    /**
     * TODO - WIP: undecided on data struct
     * @param user
     */
    public String addContact(User user){
        if(userContactList.contains(user)){
            return user.getUsername() + " already in contact list";
        }
        else{
            // add contact to list
        }
        return "operation failed";
    }
    /**
     * Attempts to establish a connection and register a user to the server.
     * @param username user
     * @param path path of image to be sent to server
     */
    public void connectToServer(String username, String path) {
        // empty as of now
        ClientConnect connect = new ClientConnect(username, path);
        Future<?> connectTask = connectAndReceiveExecutor.submit(connect);
        while (true) {
            // important: returns true even if an exception was encountered,
            // connectTask.get() returns null if completed successfully
            if (connectTask.isDone()) {

                break;
            }
        }
        connectAndReceiveExecutor.execute(new ReceiveMessage());
    }

    /**
     * disconnects the user and closes the socket, invoked by gui.
     */
    public void disconnectFromServer() {
        System.out.println("disconnect");
        connectAndReceiveExecutor.shutdownNow();
        ExecutorService disconnectClient = Executors.newSingleThreadExecutor();
        disconnectClient.execute(new ClientDisconnect(clientSocket.getRemoteSocketAddress().toString()));
    }

    /**
     * TODO - WIP
     * method overloading for send chat msg functions,
     * since a Message can contain: Text OR Image OR Text AND Image
     *
     * @param message String, any alphanumeric + special character
     * @param icon    Image, jpg or png
     */
    public void sendChatMsg(String message, ImageIcon icon, ArrayList<User> recipients) {
        // consider storing messages in some data struct.
        // user == client == author of message.
        if (user != null) {
            Message imageTextMsg = new Message(message, icon, user, recipients, MessageType.TEXT_IMAGE);

            //todo will probably implement future object,
            // so a client can't send another message before the first
            // has successfully been written to ObjectOutputStream
            sendMessageExecutor.execute(new SendMessage(imageTextMsg));
        }
    }

    /**
     * TODO
     * @param message chat message only containing text
     */
    public void sendChatMsg(String message) {

    }

    /**
     * TODO
     * @param icon chat message only containing an image
     */
    public void sendChatMsg(ImageIcon icon) {

    }

    /**
     * Runnable for reading messages from server.
     * <p>
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
                    System.out.println("reading from server");
                    is = clientSocket.getInputStream();
                    ois = new ObjectInputStream(is);
                    Object o = ois.readObject();

                    if (o instanceof List<?>) {
                        handleListResponse(o, userOnlineList);
                        // call the interface after response has been handled
                        connectionHandler.usersUpdatedCallback(userOnlineList);
                    } else if (o instanceof Message) {
                        handleMessageResponse(o);
                    }

                    Thread.sleep(250);
                } catch (IOException e){
                    exceptionHandler(e, Thread.currentThread(), "");
                } catch (InterruptedException e){
                    exceptionHandler(e, Thread.currentThread(), " Thread interrupted");
                } catch (ClassNotFoundException e){
                    exceptionHandler(e, Thread.currentThread(), " error reading from server");
                }
            }
        }
    }

    /**
     * Distinguishing two generic objects by their
     * generic parameter is simply not something you can do in Java,
     * so Receive message has to invoke this mess of a solution.
     *
     * @param o Object read from ObjectInputStream
     */
    private synchronized void handleListResponse(Object o, ArrayList<User> list) {
        list.clear(); // to avoid {user1}, {user1, user1, user2}
        System.out.println("\nCLIENT");
        for (Object element : (List<?>) o) {
            if (element instanceof User) {
                list.add((User) element);
                System.out.println(element.toString() + "added to list in client " + clientSocket.getLocalPort());
            }
            // here we can add functionality for other List<?> responses
        }
    }

    /**
     * @param o takes in a message object from OOS,
     * fires the implementation which corresponds to the type of (Message) Object o.
     */
    private synchronized void handleMessageResponse(Object o) {
        Message message = (Message) o;
        switch (message.getType()) {
            case TEXT -> msgReceivedHandler.textMessageReceived(message);
            case IMAGE -> msgReceivedHandler.imageMessageReceived(message);
            case TEXT_IMAGE -> msgReceivedHandler.txtAndImgMessageReceived(message);
        }
    }

    /**
     *
     */
    private class SendMessage implements Runnable {
        public SendMessage(Message message) {
            try{
                oos.writeObject(message);
                oos.flush();
            } catch (IOException e) {
                exceptionHandler(e, Thread.currentThread(), " failed to send message" );
            }
        }

        @Override
        public void run() {
        }
    }

    /**
     *
     */
    private class ClientConnect implements Runnable {
        private final String username;
        private final String path;

        public ClientConnect(String username, String imgPath) {
            this.username = username;
            this.path = imgPath;
        }

        @Override
        public void run() {
            try {
                // path --> bufferedImg --> bytearr

                BufferedImage bufferedImage = ImageIO.read(new File(path));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", baos);
                baos.flush();
                byte[] imageInByteArr = baos.toByteArray();
                baos.close();

                user = new User(username,imageInByteArr);
                InetAddress address = InetAddress.getByName(ip);
                clientSocket = new Socket(address, port);
                os = clientSocket.getOutputStream();
                //BufferedOutputStream bos = new BufferedOutputStream(os);
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(user);
                oos.flush();

                // notify gui that connection is established
                connectionHandler.connectionOpenedCallback("Success established connection to: " + clientSocket.getInetAddress().toString(), user);
                // end of runnable
            } catch (IOException e) {
                exceptionHandler(e, Thread.currentThread(), "failed to connect");
            }
        }
    }

    /**
     *
     */
    private class ClientDisconnect implements Runnable {
        public ClientDisconnect(String client){

        }
        @Override
        public void run() {
            try {
                if (is != null) {
                    is.close();
                }
                if (ois != null) {
                    ois.close();
                }
                if (os != null) {
                    os.close();
                }
                if (oos != null) {
                    oos.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                    connectionHandler.connectionClosedCallback("You've been disconnected");
                }
            } catch (IOException e) {
                System.out.println("failed to close socket");
                e.printStackTrace();
            }
        }
    }

    /**
     * placeholder func
     *
     * @param e Exception to handle
     * @param t Thread in which exception occurred
     */
    public void exceptionHandler(Exception e, Thread t, String messageToGUI) {
        // javadoc says "Thrown to indicate that there is an error creating or accessing a Socket."
        if (e instanceof SocketException) {
            if(e instanceof ConnectException){
                connectionHandler.exceptionCallback(e, messageToGUI);
            }
            else if (e instanceof BindException){
                connectionHandler.exceptionCallback(e, "port likely in use");
            }
            else if(e instanceof NoRouteToHostException){
                connectionHandler.exceptionCallback(e, "check firewall permissions");
            }
        }

        else if (e instanceof InterruptedException) {
            // this is expected, some threads need to be killed and will be interrupted
            // no need to notify user
            System.out.println(t.getName() + messageToGUI);
        }

        else if (e instanceof ClassNotFoundException){
            System.out.println(t.getName() + messageToGUI);
        }
    }
    public User getUser() {
        return user;
    }

    public String getServerIP() {
        return ip;
    }

    public int getServerPort() {
        return port;
    }

    public int getLocalPort() {
        if (clientSocket != null) {
            return clientSocket.getLocalPort();
        }
        return -1;
    }
}
