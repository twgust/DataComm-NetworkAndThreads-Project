package server.Controller.Buffer;

import entity.User;

import java.util.HashSet;

/**
 * @author twgust
 */
public class UserBuffer {
    private HashSet<User> onlineBuffer;
    public UserBuffer() {
        onlineBuffer = new HashSet<>();
    }

    public synchronized void put(User user){
        onlineBuffer.add(user);
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
