package server.controller;

import entity.User;
import entity.LoggerUtil;

import java.net.Socket;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;




/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
public class Buffer {
    private final HashMap<User, Socket> clientBuffer;
    private final Logger log;

    /**
     * TODO
     */
    public Buffer(){
        clientBuffer = new HashMap<>();
        log = Logger.getLogger("Buffer");
    }

    /**
     * TODO
     * @param user K
     * @param client V
     */
    protected synchronized void put(User user, Socket client){
        clientBuffer.put(user,client);
        log.log(Level.INFO, LoggerUtil.ANSI_BLUE + "Client - {username='" + user.getUsername() +"'"
                + "socket='" + client.getRemoteSocketAddress() +"'}"
                + " connected to server\n" + LoggerUtil.ANSI_BLUE);
        notifyAll();
    }

    /**
     * TODO
     * @param user key to fetch
     * @return returns value for K: user
     * @throws InterruptedException
     */
    protected synchronized Socket get(User user) throws InterruptedException{
        while(clientBuffer.isEmpty()){
            log.log(Level.OFF, LoggerUtil.ANSI_BLUE+"buffer empty, waiting for clients to be added to buffer...\n"
            + LoggerUtil.ANSI_BLUE);
            wait();
        }
        return clientBuffer.get(user);
    }

    /**
     *
     * @param user user to be removed, invoked on disconnect
     * @throws InterruptedException
     */
    protected synchronized void removeUser(User user) throws InterruptedException{
        while(clientBuffer.isEmpty()){
            wait();
        }
        clientBuffer.remove(user);
    }

    /**
     * @param user the user to look up.
     * @return true if user contains buffer
     */
    protected synchronized boolean hasUser(User user){
        return clientBuffer.containsKey(user);
    }


    /**
     * @return size, represents nr of clients connected.
     */
    protected synchronized int size(){
        return clientBuffer.size();
    }
    protected synchronized void printAllUsers(){
        System.out.println(clientBuffer.keySet());
    }
    protected synchronized Set<User> getKeySet(){
       return  clientBuffer.keySet();
    }
}
