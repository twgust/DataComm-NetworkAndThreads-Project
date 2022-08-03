package server.controller.Buffer;

import entity.User;
import server.Entity.Client;


import java.util.HashMap;
import java.util.Set;




/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
public class ClientBuffer {
    private HashMap<User, Client> clientBuffer;

    /**
     * Buffer, a thread-safe hashmap implementation containing the connected clients
     * K = User, not by reference but by string since .equals has been overridden.
     * V = Client
     * Not accessible to clients since that would enable clients to perform operations on other Clients(socket)
     */
    public ClientBuffer(){
        clientBuffer = new HashMap<>();
    }

    /**
     * @param user K
     * @param client V
     */
    public synchronized void put(User user, Client client){
        clientBuffer.put(user,client);
        notifyAll();
    }

    /**
     * @param user key to fetch
     * @return returns value for K: user
     * @throws InterruptedException if wait() is interrupted exception is thrown
     */
    public synchronized Client get(User user) throws InterruptedException{
        if(clientBuffer.isEmpty()){
            wait();
        }
        return clientBuffer.get(user);
    }

    /**
     *
     * @param user user to be removed, invoked on disconnect
     * @throws InterruptedException if wait() is interrupted exception is thrown
     */
    public synchronized void removeUser(User user) throws InterruptedException{
        if(clientBuffer.isEmpty()){
            wait();
        }
        clientBuffer.remove(user);
    }

    /**
     * @param user the user to look up.
     * @return true if user contains buffer
     */
    public synchronized boolean hasUser(User user){
        return clientBuffer.containsKey(user);
    }


    /**
     * @return size, represents nr of clients connected.
     */
    public synchronized int size(){
        return clientBuffer.size();
    }

    /**
     * test func for now
     */
    public synchronized void printAllUsers(){
        System.out.println(clientBuffer.keySet());
    }

    /**
     * @return the complete keySet of buffer
     */
    public synchronized Set<User> getKeySet(){
       return  clientBuffer.keySet();
    }
}
