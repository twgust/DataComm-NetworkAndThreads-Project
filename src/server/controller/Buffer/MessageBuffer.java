package server.controller.Buffer;

import entity.Message;
import entity.UserSet;

import java.util.HashSet;
import java.util.LinkedList;

public class MessageBuffer {
    private LinkedList<HashSet<UserSet>> messages;

    public MessageBuffer(){
        messages = new LinkedList<>();
    }
    public synchronized HashSet<UserSet> getMessage() throws InterruptedException {
        if(messages.isEmpty()){
            wait();
        }
        return messages.removeFirst();
    }
    public synchronized void queueMessage(HashSet<UserSet> message){
        messages.addLast(message);
        notifyAll();
    }
}
