package server.controller;

import entity.User;

public interface UserConnection {
    void onUserDisconnectListener(User user);
    void onUserConnectListener(User user);
}
