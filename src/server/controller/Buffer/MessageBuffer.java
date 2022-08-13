package server.controller.Buffer;

import entity.Message;
import entity.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author twgust
 * Class for saving and fetching messages that weren't able to be sent due to some IOException between client-server
 */
public class MessageBuffer {
    private final HashMap<Message, User> messages;

    public MessageBuffer( ){
        messages = new HashMap<>();
    }

    /**
     * gets the user which the message couldn't be sent to
     * @param message the Key
     * @return V = User
     * @throws InterruptedException
     */
    public synchronized User getUser(Message message) throws InterruptedException {
        if(messages.isEmpty()){
            wait();
        }
        return messages.get(message);
    }

    /**
     * @author twgust
     * @param  user for which you'd like to fetch unsent messages
     * @return a list of the unsent messages
     */
    public synchronized List<Message> getAllUnsentMessages(User user){
        if(messages.isEmpty()){
            return null;
        }
        // 1) check first if buffer has a message for user
        if(messages.containsValue(user)){
            // 2) if it does then start iterating of the keyset (message)
            List<Message> unsentMessages = new ArrayList<>();
            messages.keySet().iterator().forEachRemaining(message -> {
                // 3) for each message, check if recipient list contains the user object passed into function
                if(message.getRecipientList().contains(user)){
                    // 4) if true, then add it to the list that is to be returned.
                    unsentMessages.add(message);
                }
            });
            return unsentMessages;
        }
        return null;
    }

    /**
     * @author twgust
     * @param message to be removed from buffer, typically invoked for each message in getAllUnsentMessages(User user)
     * @throws InterruptedException
     */
    public synchronized void remove(Message message) throws InterruptedException {
        if(messages.isEmpty()){
            wait();
        }
        else{
            messages.remove(message);
        }
    }

    /**
     * Adds a message to the back of the queue, fifo
     * @param message message to queue
     */
    public synchronized void put( Message message,User user){
        messages.put(message, user);
        System.out.println(messages.keySet().size() + " keyset");
        System.out.println(messages.size() + " buffer size");
        notifyAll();
    }
}
