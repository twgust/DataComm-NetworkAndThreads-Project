package server.controller;

import entity.User;

import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;




/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
public class Buffer {
    private final HashMap<User, Socket> clientBuffer;
    private final Logger log;

    public Buffer(){
        clientBuffer = new HashMap<>();
        log = Logger.getLogger("Buffer");
    }

    protected synchronized void put(User user, Socket client){
        clientBuffer.put(user,client);
        log.log(Level.INFO, "{username='" + user.getUsername() +"'"
                + "socket='" + client.getRemoteSocketAddress() +"'}"
                + " connected to server");
        notifyAll();
    }

    protected synchronized Socket get(User user) throws InterruptedException{
        while(clientBuffer.isEmpty()){
            log.log(Level.INFO, "buffer empty, waiting for clients to be added to buffer...");
            wait();
        }
        return clientBuffer.get(user);
    }

    /**
     * @return size, represents nr of clients connected.
     */
    protected synchronized int size(){
        int clients = clientBuffer.size();
        log.log(Level.INFO, "#" + clients + " connected to server");
        return clients;
    }
    protected synchronized void printAllUsers(){
        System.out.println(clientBuffer.keySet());
    }
}
