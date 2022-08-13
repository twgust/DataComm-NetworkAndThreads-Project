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
    private final LinkedList<Message> messages;

    public MessageBuffer( ){
        messages = new LinkedList<>();
    }

    /**
     * @return Returns a message from buffer according to fifo principle
     * @throws InterruptedException if thread is interrupted
     */
    public synchronized Message getMessage() throws InterruptedException {
        if(messages.isEmpty()){
            wait();
        }
        return messages.removeFirst();
    }

    /**
     * Adds a message to the back of the queue, fifo
     * @param message message to queue
     */
    public synchronized void queueMessage(Message message){
        messages.addLast(message);
        notifyAll();
    }

    public LinkedList<Message> getMessages() {
        return messages;
    }
}
