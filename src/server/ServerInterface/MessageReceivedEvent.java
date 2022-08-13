package server.ServerInterface;

import entity.Message;

/**
 * @author twgust
 */
public interface MessageReceivedEvent {
    void onMessageReceivedEvent(Message message);

}
