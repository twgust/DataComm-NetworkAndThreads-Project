package server.ServerInterface;

import entity.Message;

public interface MessageReceivedEvent {
    void onMessageReceivedEvent(Message message);

}
