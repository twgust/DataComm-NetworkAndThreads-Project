package client.controller;

import entity.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Notes and Requirements for Client:
 * <p>
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 * <p>
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Client:
 * <p>
 * 1, Connect to server: [x]
 * 2, Create a username and profile picture before connecting: [x]
 * 3, Disconnect from server: [x]
 * <p>
 * 3, Send messages to User(s) - through Server: [x]
 * 4, Receive messages from User(s) - through Server: [x]
 * <p>
 * 5, Display connected User(s): [X]
 * 6, Add connected users to a Contact list: [X] <--
 * 7, Save contact list on local storage ON client disconnect and exit:  [X] <--
 * 8, Load contact list from local storage On client connect and startup: [X] <--
 * 9, Select recipients of message from Contact list: [X] <--
 * 10, Select recipients of message from Online list: [x] <---
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

    private IConnectionHandler connectionHandler;
    private IMessageReceivedHandler msgReceivedHandler;


    // A list containing all currently online users
    // Loaded from local storage on startup, Saved and updated to local storage on exit
    private HashSet<User> onlineUserHashSet;
    private HashSet<User> userContactList;


    private String ip;
    private int port;

    private  Socket clientSocket;

    private  ObjectInputStream ois;
    private  InputStream is;
    private  ObjectOutputStream oos;
    private  OutputStream os;
    private ContactListFileHandler contactFileHandler;

    // threads.
    private ExecutorService threadPool;
    private ReceiveMessage receiveMessage;
    private SendMessage sendMessage;
    private ClientConnect connect;
    private User user;
    private String id;
    private ObjectBuffer objectBuffer;


    /**
     * @author twgust
     */
    public ClientController(String id) {
        this.id = id;

        // since connection is only done once, we might as well reuse that single Thread.
    }

    /**
     * @author twgust
     * @param impl of interface from ClientGUI
     */
    public void addConnectionHandler(IConnectionHandler impl) {
        this.connectionHandler = impl;
    }
    /**
     * @author twgust
     * @param impl of interface from ClientGUI
     */
    public void addMessageReceivedHandler(IMessageReceivedHandler impl){
        this.msgReceivedHandler = impl;
    }

    /**
     *
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
            userContactList.add(user);
            connectionHandler.contactsUpdatedCallback(userContactList);
            return "User added";
        }
    }
    public void removeContact(User user){
        userContactList.remove(user);
        connectionHandler.contactsUpdatedCallback(userContactList);
    }
    /**
     * @author twgust
     * Attempts to establish a connection and register a user to the server.
     * @param username user
     * @param path path of image to be sent to server
     */
    public void connectToServer(String username, String path, InetSocketAddress socketAddress) {
        threadPool = Executors.newFixedThreadPool(4);
        connect = new ClientConnect(username, path, socketAddress);
//        boolean success = false;
        FutureTask<String> connectTask = new FutureTask<>(connect, "good");
        threadPool.submit(connectTask);
        try{
            if (connectTask.get().equals("good")) {
//                success = true;
                connectionHandler.connectionOpenedCallback
                        ("Success established connection to: " + clientSocket.getInetAddress().toString(), user);
                // initialize the data structures
                onlineUserHashSet = new HashSet<>();
                receiveMessage = new ReceiveMessage();
                objectBuffer = new ObjectBuffer();

                // execute the runnable(s) with thread pool
                threadPool.execute(receiveMessage);
                threadPool.execute(new ResponseHandler());
                contactFileHandler = new ContactListFileHandler(user);
                userContactList = contactFileHandler.readContactFile();
                if (userContactList == null) {
                    userContactList = new HashSet<>();
                }
                connectionHandler.contactsUpdatedCallback(userContactList);
            }
        }catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }
/*        while (!success) {
            connect = new ClientConnect(username, path, connectionHandler.getIPCallback());
            connectTask = new FutureTask<>(connect, "good");
            threadPool.submit(connectTask);
            try{
                if (connectTask.get().equals("good")) {
                    success = true;
                    connectionHandler.connectionOpenedCallback
                            ("Success established connection to: " + clientSocket.getInetAddress().toString(), user);
                    // initialize the data structures
                    onlineUserHashSet = new HashSet<>();
                    receiveMessage = new ReceiveMessage();
                    objectBuffer = new ObjectBuffer();

                    // execute the runnable(s) with thread pool
                    threadPool.execute(receiveMessage);
                    threadPool.execute(new ResponseHandler());
                    contactFileHandler = new ContactListFileHandler(user);
                    userContactList = contactFileHandler.readContactFile();
                    if (userContactList == null) {
                        userContactList = new HashSet<>();
                    }
                    connectionHandler.contactsUpdatedCallback(userContactList);
                }
            }catch (InterruptedException | ExecutionException e){
                e.printStackTrace();
            }
        }*/


    }

    /**
     * @author twgust
     * Does exactly what it says
     * @param socket socket
     */
    private void setupStreams(Socket socket){
        try{
            this.os = socket.getOutputStream();
            this.oos = new ObjectOutputStream(os);
            this.is = socket.getInputStream();
            this.ois = new ObjectInputStream(is);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    /**
     * @author twgust
     * disconnects the user and closes the socket, invoked by gui.
     */
    public void disconnectFromServer() {
        ClientDisconnect clientDisconnect = new ClientDisconnect();
        FutureTask<String> disconnect = new FutureTask<>(clientDisconnect, "disconnected");
        threadPool.submit(disconnect);
        try{
            if (disconnect.get().equals("disconnected")){
                System.out.println("ok");
            }
        }catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
        }
        //threadPool.submit(new ClientDisconnect());
    }

    /**
     * @author twgust
     * @param path path of file
     * @return image represented by byte array
     * @throws IOException ByteArrayOutputStream / ImageIO
     */
    private synchronized byte[] pathToByteArr(String path) throws IOException{
        File fileToProcess = new File(path);
        String fileName = fileToProcess.getName();
        String extension = "";
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            extension = fileName.substring(extensionIndex+1);
        }
        BufferedImage bufferedImage = ImageIO.read(fileToProcess);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, extension, baos);
        baos.flush();
        byte[] imageInByteArr = baos.toByteArray();
        baos.close();
        return imageInByteArr;
    }


    /**
     * Represents a text-only chat message
     * @author twgust
     * Sends a text only Message to server, see Message class for more details.
     * @param message chat message only containing text
     */
    public synchronized void sendChatMsg(String message, Object[] recipients, MessageType msgType) throws IOException {
        assert (user != null);
        ArrayList<User> recipientList = new ArrayList<>();
        for (Object o:recipients) {
            if(o instanceof User){
                recipientList.add((User)o);
            }
        }
        //recipientList.remove(user);
        //hardcoded test solution until gui is in place
        Message message1 = new Message(message, user,recipientList,msgType);
        SendMessage sendMessage1 = new SendMessage(message1, clientSocket);
        FutureTask<String> await = new FutureTask<>(sendMessage1,"result");
        System.out.println("attempting to send message");
        threadPool.submit(await);
        while(true) {
            try {
                if (await.get().equals("result")){
                    //notify gui
                    System.out.println("message sent: <" + message + ">");
                    return;
                }
            }
            catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
        }
    }

    /**
     * Represents an image-only chat message
     * @author twgust
     * @param recipients recipients of message
     * @param msgType message type
     * @param path path to image
     * @throws IOException
     */
    public synchronized void sendChatMsg(Object[] recipients, MessageType msgType, String path) throws IOException {
        assert (user != null);
        ArrayList<User> recipientList = new ArrayList<>();

        for (Object o:recipients) {
            if(o instanceof User){
                recipientList.add((User)o);
            }
        }

        byte[] imgInByteArr = pathToByteArr(path);

        //hardcoded test solution until gui is in place
        Message message = new Message(imgInByteArr,user,recipientList,msgType);
        SendMessage sendMessage1 = new SendMessage(message, clientSocket);
        FutureTask<String> await = new FutureTask<>(sendMessage1,"result");
        System.out.println("attempting to send message");
        threadPool.submit(await);
        while(true) {
            try {
                if (await.get().equals("result")){
                    //notify gui
                    System.out.println("message sent");
                    return;
                }
            }
            catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
        }
    }

    /**
     * Represents Text & Image chat message
     * @author twgust
     * @param message contents of textmessage
     * @param recipients recipients of message
     * @param msgType type of message
     * @param path path to image
     * @throws IOException
     */
    public synchronized void sendChatMsg(String message, Object[] recipients, MessageType msgType, String path) throws IOException {
        assert (user != null);
        ArrayList<User> recipientList = new ArrayList<>();

        for (Object o:recipients) {
            if(o instanceof User){
                recipientList.add((User)o);
            }
        }

        byte[] imgInByteArr = pathToByteArr(path);
        Message message1 = new Message(message, imgInByteArr,user,recipientList,msgType);
        SendMessage sendMessage1 = new SendMessage(message1, clientSocket);
        FutureTask<String> await = new FutureTask<>(sendMessage1,"result");
        threadPool.submit(await);
        while(true) {
            try {
                if (await.get().equals("result")){
                    System.out.println("message sent");
                    return;
                }
            }
            catch (InterruptedException | ExecutionException e) {e.printStackTrace();}
        }
    }

    /**
     * @author twgust
     * Runnable for reading messages from server.
     * Reads object as generic object, gets type of object by
     * using conditional and "instance of" and then handles
     * object returned from server accordingly
     */
    private class ReceiveMessage implements Runnable {
        @Override
        public void run() {
            while (receiveMessage != null) {
                try {

                    Object o = ois.readObject();
                    if (o instanceof Sendables){
                        objectBuffer.enqueueSendable((Sendables) o);
                    }

                }
                catch (IOException | InterruptedException | ClassNotFoundException e){
                    exceptionHandler(e, Thread.currentThread(), "");
                }
            }
        }
    }

    /**
     * @author twgust
     * ResponseHandler fetches the elements from the object buffer containing Sendables
     * which it then reads and processes.
     */
    private class ResponseHandler implements Runnable{
        @Override
        public void run() {
            while(objectBuffer != null){
                Sendables objet = null;
                try {
                    objet = objectBuffer.dequeueSendable();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                switch (Objects.requireNonNull(objet).getSendableType()){
                    case Message ->{
                        Message message = (Message) objet;
                        System.out.println(message + " to  " + user);
                        handleMessageResponse(message);
                    }
                    case UserSet -> {
                        UserSet userSet = (UserSet) objet;
                        System.out.println(((UserSet) objet).getUserSet().size()  +   "-- " + clientSocket.getLocalPort());
                        handleUserHashSetResponse(userSet,onlineUserHashSet);
                        connectionHandler.usersUpdatedCallback(onlineUserHashSet);

                    }
                }
            }
        }
    }

    /**
     * @author twgust
     * Fires the implementation which corresponds to the type of (Message) Object o.
     * @param o takes in a message object from OOS,
     */
    private void handleMessageResponse(Object o) {
        Message message = (Message) o;
        switch (message.getType()) {
            case TEXT -> {
                msgReceivedHandler.textMessageReceived(message, LocalTime.now());
                System.out.println("Received text message, passing information to gui");
            }
            case IMAGE -> msgReceivedHandler.imageMessageReceived(message, LocalTime.now());
            case TEXT_IMAGE -> msgReceivedHandler.txtAndImgMessageReceived(message, LocalTime.now());
        }
    }

    /**
     * @author twgust
     * takes in an object, casts it to a UserSet obj,
     * fetches Set from UserSet obj, iterates over the set and
     * adds elements to hashset.
     * @param o Object read from ObjectInputStream
     */
    private void handleUserHashSetResponse(Object o, HashSet<User> set) {
        if(o instanceof UserSet u){
            if(u.getUserType().equals(ConnectionEventType.Connected)){
                Set<User> tmp = ((UserSet) o).getUserSet();
                set.addAll(tmp);
            }
            else if (u.getUserType().equals(ConnectionEventType.Disconnected)){
                System.out.println("disconnect type");
                set.remove(u.getHandledUser());
            }
        }
    }

    /**
     * @author twgust
     * Runnable for sending message to server
     */
    private class SendMessage implements Runnable {
        private Message message;
        private User user;
        public SendMessage(Message message, Socket socket) {
            this.message = message;
        }
        public SendMessage(User user, Socket socket){
            this.user = user;
        }

        @Override
        public void run() {
            if(clientSocket == null){
                System.out.println("try later");
                return;
            }
            try{
                if(message!=null){
                    System.out.println("sending msg");
                    oos.writeObject(message);
                    oos.flush();
                }
                else if (user!= null){
                    oos.writeObject(user);
                    oos.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                exceptionHandler(e, Thread.currentThread(), "");
            }
        }
    }

    /**
     * @author twgust
     * Connects client to server and uploads an instance of User(String userName, Byte[] img]
     */
    private class ClientConnect implements Runnable {
        private final String username;
        private final String path;
        private final InetSocketAddress socketAddress;

        /**
         * Constructor, input parameters fetched from listening to ui components
         * @param username string representation of a username
         * @param imgPath string representation of path of image (user avatar)
         */
        public ClientConnect(String username, String imgPath, InetSocketAddress socketAddress) {
            this.username = username;
            this.path = imgPath;
            this.socketAddress = socketAddress;
            ip = socketAddress.getAddress().toString();
            port = socketAddress.getPort();
        }

        /**
         * see inline comments for step-by-step walk through
         */
        @Override
        public void run() {


            try {
                // step 1: read image from file
                BufferedImage bufferedImage = ImageIO.read(new File(path));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "jpg", baos);
                baos.flush();
                byte[] imageInByteArr = baos.toByteArray();
                baos.close();

                // step 2: create user object
                user = new User(username,imageInByteArr);

                // step 3: establish connection to server
                clientSocket = new Socket(socketAddress.getAddress(), socketAddress.getPort());
                setupStreams(clientSocket);

                // step 4: write user object to server
                oos.writeObject(user);
                oos.flush();
                System.out.println(clientSocket.getLocalPort());

            } catch (IOException e) {
                e.printStackTrace();
                exceptionHandler(e, Thread.currentThread(), "failed to connect");
            }


        }
    }

    /**
     * @author twgust
     * Checks status of opened streams,
     * closes them and notifies client through a GUI update.
     */
    private class ClientDisconnect implements Runnable {
        /**
         * default constructor
         */
        public ClientDisconnect(){
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
                    objectBuffer = null;
                    receiveMessage = null;
                    onlineUserHashSet = null;


                    clientSocket.close();
                    clientSocket = null;
                    System.out.println("disconnected");
                    //clear hash set and tell the interface to update gui
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
              //  disconnectFromServer();
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

    /**
     *
     * @return the user registered from client
     */
    public User getUser() {
        return user;
    }

    /**
     * @return ip of server
     */
    public String getServerIP() {
        return ip;
    }

    /**
     * @return port of server
     */
    public int getServerPort() {
        return port;
    }

    /**
     *
     * @return port of client
     */
    public int getLocalPort() {
        if (clientSocket != null) {
            return clientSocket.getLocalPort();
        }
        return -1;
    }

    public void writeContactFile() {
        contactFileHandler.writeContactFile(userContactList);
    }
}
