package server.controller.Threads;

import entity.User;
import server.Entity.Client;
import server.controller.Buffer.ClientBuffer;
import server.ServerInterface.LoggerCallBack;
import server.ServerInterface.UserConnectionCallback;
import server.controller.ServerLogger;

import javax.imageio.ImageIO;
import javax.net.SocketFactory;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.logging.Level;

/**
 * Concurrent Server
 */
public class ServerMainThread implements Runnable{
    private final ClientBuffer buffer;
    private final ServerLogger logger;
    private final UserConnectionCallback userConnectionCallback;


    public ServerMainThread(ClientBuffer buffer, ServerLogger logger, UserConnectionCallback userConnectionCallback){
        this.buffer = buffer;
        this.logger = logger;
        this.userConnectionCallback = userConnectionCallback;

    }
    @Override
    public void run(){
        try {
            ServerSocket serverSocket = null;
            String logStartServerMsg;
            while(serverSocket == null){
                try{
                    int newPort =  Integer.parseInt(JOptionPane.showInputDialog(null, "enter new port"));
                    // log to server gui
                    logStartServerMsg = "attempting to initialize server on port: [" + newPort + "]";
                    logger.logEvent(Level.INFO, logStartServerMsg, LocalTime.now());
                    serverSocket = new ServerSocket(newPort);

                }catch (BindException e){
                    logStartServerMsg = "attempt failed, port is busy";
                    logger.logEvent(Level.WARNING, logStartServerMsg, LocalTime.now());
                }
            }
            System.out.println("ok");
            String logServerRunningMsg = " server running on port: [" + serverSocket.getLocalPort() + "]";
            logger.logEvent(Level.INFO, logServerRunningMsg, LocalTime.now());

                // multithreaded server
                while(true){
                    Socket clientSocket = serverSocket.accept();
                    // start timer after client connection accepted
                    long start = System.currentTimeMillis();

                    // set up streams for client
                    InputStream is = clientSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);
                    OutputStream os = clientSocket.getOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(os);

                    User user = null;
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
                        user = (User) oUser;
                        byte[] imgInBytes = user.getAvatarAsByteBuffer();

                        // Step 4) buffer the img and write it to server storage
                        ByteArrayInputStream bais = new ByteArrayInputStream(imgInBytes);
                        BufferedImage img = ImageIO.read(bais);
                        String path = "src/server/avatars/"+ user.getUsername() + ".jpg";
                        ImageIO.write(img, "jpg", new File(path)); //Save the file, works!

                        // Step 5) put the buffer in the user, enabling server to perform operations on client
                        Client client = new Client(clientSocket, oos, ois);
                        buffer.put(user, client);

                        // start a separate thread which listens to client
                        // step 6) fire implementation of userConnectionCallback

                        // log to server gui
                        long end = (System.currentTimeMillis() - start);
                        String logClientTimeToConnectMsg = "finished processing client [" + clientSocket.getLocalAddress() + "] in " + end + "ms";
                        logger.logEvent(Level.INFO, logClientTimeToConnectMsg, LocalTime.now());

                        // log to server gui
                        String logUserConnectedMsg = user.getUsername() + " connected to server";
                        logger.logEvent(Level.INFO, logUserConnectedMsg, LocalTime.now());
                        userConnectionCallback.onUserConnectListener(user);
                    }
                }
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();}
        }
    }

