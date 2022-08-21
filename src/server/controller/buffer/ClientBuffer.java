package server.controller.buffer;

import entity.User;
import server.entity.Client;


import java.util.Collection;
import java.util.HashMap;
import java.util.Set;




/*
 * Since HashMap isn't thread-safe we use a synchronized buffer implementation
 * to perform operations on the data structure
 */
/**
 * @author twgust
 */
public class ClientBuffer {
    private final HashMap<User, Client> clientBuffer;

    /**
     * @author twgust
     * Buffer, a thread-safe hashmap implementation containing the connected clients
     * K = User, not by reference but by string since .equals has been overridden.
     * V = Client
     * Not accessible to clients since that would enable clients to perform operations on other Clients(socket)
     */
    public ClientBuffer(){
        clientBuffer = new HashMap<>();
    }

    /**
     * @author twgust
     * @param user K
     * @param client V
     */
    public synchronized void put(User user, Client client){
        clientBuffer.put(user,client);
        notifyAll();
    }

    /**
     * @author twgust
     * @param user key to fetch
     * @return returns value for K: user
     */
    public synchronized Client get(User user){
        if(clientBuffer.isEmpty()){
            try{
                wait();
            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }
        return clientBuffer.get(user);
    }
    public synchronized Collection<Client> allValues(){
        return clientBuffer.values();
    }

    /**
     * @author twgust
     * @param user user to be removed, invoked on disconnect
     * @throws InterruptedException if wait() is interrupted exception is thrown
     */
    public synchronized void removeUser(User user) throws InterruptedException{
        if(clientBuffer.isEmpty()){
            wait();
            if(Thread.interrupted()){
                throw new InterruptedException();
            }
        }
        clientBuffer.remove(user);
    }

    /**
     * NOT USED
     * @author twgust
     * @param user the user to look up.
     * @return true if user contains buffer
     */
    public synchronized boolean hasUser(User user){
        return clientBuffer.containsKey(user);
    }


    /**
     * @author twgust
     * @return size, represents nr of clients connected.
     */
    public synchronized int size(){
        return clientBuffer.size();
    }

    /**
     * NOT USED
     * @author twgust
     * @return the complete keySet of buffer
     */
    public synchronized Set<User> getKeySet(){
       return  clientBuffer.keySet();
    }
}
