package server.controller.Buffer;

import entity.Message;

import java.util.LinkedList;
import java.util.concurrent.FutureTask;

/**
 * linked list gives o(1) addLast and addFirst, implements queue interface
 * and doesn't have aggressive resizing like arraylist.
 * Since
 */
public class MessageBuffer {
    public LinkedList<FutureTask> getMessages() {
        return messages;
    }

    private final LinkedList<FutureTask> messages;

    public MessageBuffer( ){
        messages = new LinkedList<>();
    }

    /**
     * @return Returns a message from buffer according to fifo principle
     * @throws InterruptedException if thread is interrupted
     */
    public synchronized FutureTask getMessage() throws InterruptedException {
        if(messages.isEmpty()){
            wait();
        }
        return messages.removeFirst();
    }

    /**
     * Adds a message to the back of the queue, fifo
     * @param message message to queue
     */
    public synchronized void queueMessage(FutureTask message){
        messages.addLast(message);
        notifyAll();
    }
}
