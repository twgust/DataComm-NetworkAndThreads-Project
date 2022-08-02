package server.controller;

import entity.Message;

import java.net.Socket;

public interface MessageReceivedListener {
    void onMessageReceived(Message message, Socket client);
}
