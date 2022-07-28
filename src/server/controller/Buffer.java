package server.controller;

import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  socket.getRemoteSocketAddress.toString() should work quite well as a key for the client Map<String,Socket>
 *  since no two connected clients (sockets) can have the same ip address and port combination.
 *  Even if two clients from the same local network connect to the server,
 *  E.g. 193.29.107.246, their port will differ:
 *  client 1: 193.29.107.246:62353
 *  client 2: 193.29.107.246:62354
 *  resulting in a different hashcode.


/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
public class Buffer {
    private final HashMap<String, Socket> clientBuffer;
    private final Logger logger;

    public Buffer(){
        clientBuffer = new HashMap<>();
        logger = Logger.getLogger(Buffer.class.getName());
    }

    protected synchronized void put(String clientInetAddress, Socket client){
        clientBuffer.put(clientInetAddress,client);
        logger.log(Level.INFO, "CLIENT " + clientInetAddress + " connected to server");
        notifyAll();
    }

    protected synchronized Socket get(String clientInetAddress) throws InterruptedException{
        while(clientBuffer.isEmpty()){
            logger.log(Level.INFO, "buffer empty, waiting for clients to be added to buffer...");
            wait();
        }
        return clientBuffer.get(clientInetAddress);
    }
    protected synchronized int size(){
        return clientBuffer.size();
    }
}
