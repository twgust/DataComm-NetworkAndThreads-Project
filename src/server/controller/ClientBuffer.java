package server.controller;

import entity.User;


import java.io.Serializable;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;




/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
public class ClientBuffer {
    private HashMap<User, Socket> clientBuffer;

    /**
     * Buffer, a thread-safe hashmap implementation containing the connected clients
     * K = User, not by reference but by string since .equals has been overridden.
     * V = Socket, socket through which the User has connected to the server.
     * Not accessible to clients since that would enable clients to perform operations on other Clients(socket)
     */
    public ClientBuffer(){
        clientBuffer = new HashMap<>();
    }

    /**
     * @param user K
     * @param client V
     */
    protected synchronized void put(User user, Socket client){
        clientBuffer.put(user,client);
        notifyAll();
    }

    /**
     * @param user key to fetch
     * @return returns value for K: user
     * @throws InterruptedException if wait() is interrupted exception is thrown
     */
    protected synchronized Socket get(User user) throws InterruptedException{
        while(clientBuffer.isEmpty()){
            wait();
        }
        return clientBuffer.getOrDefault(user, null);
    }

    /**
     *
     * @param user user to be removed, invoked on disconnect
     * @throws InterruptedException if wait() is interrupted exception is thrown
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

    /**
     * test func for now
     */
    protected synchronized void printAllUsers(){
        System.out.println(clientBuffer.keySet());
    }

    /**
     * @return the complete keySet of buffer
     */
    protected synchronized Set<User> getKeySet(){
       return  clientBuffer.keySet();
    }
}
