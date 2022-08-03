package server.controller.Threads;

import entity.User;
import server.controller.*;
import server.controller.Buffer.ClientBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.logging.Level;

/**
 * Concurrent Server
 */
public class ServerMainThread implements Runnable{
        private final int port;
        private final ClientBuffer buffer;
        private final LoggerCallBack loggerCallBack;
        private final UserConnectionCallback userConnectionCallback;


        public ServerMainThread(int port, ClientBuffer buffer, LoggerCallBack loggerCallBack, UserConnectionCallback userConnectionCallback){
            this.port = port;
            this.buffer = buffer;
            this.loggerCallBack = loggerCallBack;
            this.userConnectionCallback = userConnectionCallback;

        }
        @Override
        public void run(){
            try {

                ServerSocket serverSocket = new ServerSocket(port);
                String logServerRunningMsg = " server running on port: [" + port + "]";
                loggerCallBack.logInfoToGui(Level.INFO, logServerRunningMsg, LocalTime.now());

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
                        loggerCallBack.logInfoToGui(Level.INFO, logClientTimeToConnectMsg, LocalTime.now());

                        // log to server gui
                        String logUserConnectedMsg = user.getUsername() + " connected to server";
                        loggerCallBack.logInfoToGui(Level.INFO, logUserConnectedMsg, LocalTime.now());
                        userConnectionCallback.onUserConnectListener(user);
                    }
                }
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();}
        }
    }

