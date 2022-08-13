package server.controller.Buffer;

import entity.User;

import java.util.HashSet;
import java.util.Set;
/**
 * @author twgust
 */
public class UserBuffer {
    private HashSet<User> onlineBuffer;
    private User user;

    public UserBuffer(){
        onlineBuffer = new HashSet<>();
    }

    public synchronized void put(User user){
        onlineBuffer.add(user);
        this.user = user;
        notify();
    }

    public synchronized void remove(User user) throws InterruptedException {
        if(onlineBuffer.isEmpty()){
            wait();
        }
    }
    public synchronized HashSet<User> getHashSet(){
        return onlineBuffer;
    }
}
