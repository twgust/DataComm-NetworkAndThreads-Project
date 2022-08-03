package server.controller.ServerInterface;

import entity.User;

/**
 * implemented by ServerGUI, so the server GUI can be updated without degrading architecture of software
 * implemented by ServerController, so the server can update the clients Contact lists
 */
public interface UserConnectionCallback {
    void onUserDisconnectListener(User user);
    void onUserConnectListener(User user);
}
