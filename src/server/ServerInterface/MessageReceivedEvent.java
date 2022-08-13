package server.ServerInterface;

import entity.Message;

/**
 * @author twgust
 * implemented by Controller
 */
public interface MessageReceivedEvent {
    void onMessageReceivedEvent(Message message);

}
