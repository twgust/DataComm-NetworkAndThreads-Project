package server.controller.ServerInterface;

import entity.Message;

import java.io.ObjectOutputStream;
import java.net.Socket;

public interface MessageReceivedListener {
    void onMessageReceived(Message message, Socket client, ObjectOutputStream oos);
}
