package server.controller;

import entity.User;

public interface UserConnectionCallback {
    void onUserDisconnectListener(User user);
    void onUserConnectListener(User user);
}
