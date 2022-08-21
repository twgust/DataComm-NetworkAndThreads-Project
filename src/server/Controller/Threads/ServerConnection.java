package server.Controller.Threads;

import entity.User;
import server.Entity.Client;
import server.RunServer;
import server.Controller.Buffer.ClientBuffer;
import server.ServerInterface.UserConnectionEvent;
import server.Controller.ServerLogger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;

/**
 * @author twgust
 * Multi-Threaded server
 */
public class ServerConnection implements Runnable{
    private final UserConnectionEvent userConnectionEvent;
    private final ClientBuffer clientBuffer;
    private final ServerLogger logger;

    private ThreadPoolExecutor serverMainExecutor;

    public ServerConnection(ServerLogger logger, ClientBuffer buffer, UserConnectionEvent userConnectionEvent){
        this.logger = logger;
        this.clientBuffer = buffer;
        this.userConnectionEvent = userConnectionEvent;
    }

    /**
     * @param singleThreadExecutor executor set from controller
     */
    public void setSingleThreadExecutor(ThreadPoolExecutor singleThreadExecutor){
        this.serverMainExecutor = singleThreadExecutor;
    }

    /**
     * invoked from controller and starts the server
     */
    public void startServer(){
        serverMainExecutor.execute(this);
    }

    /**
     * Runnable for server,
     * continuous while-loop which accepts incoming connection requests until told not to
     */
    @Override
    public void run(){
        try {
            ServerSocket serverSocket = null;
            String logStartServerMsg;
            String thread = Thread.currentThread().getName();

            while(serverSocket == null){
                try{
                    int newPort =  Integer.parseInt(JOptionPane.showInputDialog(null, "enter new port"));
                    // log to server gui
                    logStartServerMsg = "attempting to initialize server on port: [" + newPort + "]";
                    logger.logEvent(Level.INFO,thread, logStartServerMsg, LocalTime.now());
                    serverSocket = new ServerSocket(newPort);

                }catch (BindException e){
                    logStartServerMsg = "attempt failed, port is busy";
                    logger.logEvent(Level.WARNING,thread, logStartServerMsg, LocalTime.now());
                }
            }
            String logServerRunningMsg = "server running on port: [" + serverSocket.getLocalPort() + "]";
            logger.logEvent(Level.INFO,thread, logServerRunningMsg, LocalTime.now());


                // multithreaded server, one iteration = one client processed
                while(true){
                    Socket clientSocket = serverSocket.accept();
                    // start timer after client connection accepted
                    String ip = "[" +  clientSocket.getLocalAddress().toString() + ":" + clientSocket.getLocalPort() + "]";
                    String logNewClientConnectionStart = thread + " Executing -> [TASK: CLIENT-CONNECTION, STATE: RUNNING]" +
                            " >> new connection from: " + ip;
                    long start = System.currentTimeMillis();

                    InputStream is = clientSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    OutputStream os = clientSocket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);

                    Object oUser = null;

                    // Step 1) read object
                    try{
                        oUser = ois.readObject();
                    }catch (EOFException e){
                        e.printStackTrace();
                    }

                    // Step 2) check type of generic object oUser since the client can other objects through the stream
                    if(oUser instanceof User){

                        // Step 3) cast the generic to User and read the byte[]
                        User user = (User) oUser;
                        byte[] imgInBytes = user.getAvatarAsByteBuffer();

                        // Step 4) buffer the img and write it to server storage
                        ByteArrayInputStream bais = new ByteArrayInputStream(imgInBytes);
                        BufferedImage img = ImageIO.read(bais);
                        String folder = RunServer.getProgramPath2();
                        String fileSeparator = System.getProperty("file.separator");
                        String newDir = folder + fileSeparator + "User Avatars" + fileSeparator;
                        ImageIO.write(img, "jpg", new File(newDir)); //Save the file, works!

                        // Step 5) put the buffer in the user, enabling server to perform operations on client
                        Client client = new Client(user,clientSocket, oos, ois);
                        clientBuffer.put(user, client);

                        // log to server gui
                        long end = (System.currentTimeMillis() - start);
                        String logNewClientConnectionEnd =  "Executing -> [TASK: CLIENT-CONNECTION, STATE:RUNNING]" +
                                " >> finished processing client [" + ip + "] in " + end + "ms!";
                        logger.logEvent(Level.INFO,thread, logNewClientConnectionEnd, LocalTime.now());

                        // step 6) fire implementation of userConnectionCallback
                        userConnectionEvent.onUserConnectListener(user);
                    }
                }
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();}
        }
    }

