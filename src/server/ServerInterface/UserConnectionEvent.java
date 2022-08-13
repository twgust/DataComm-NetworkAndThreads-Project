package server.ServerInterface;

import entity.User;
import server.Entity.Client;

/**
 * @author twgust
 * implemented by ServerGUI, so the server GUI can be updated without degrading architecture of software
 * implemented by ServerController, so the server can update the clients Contact lists
 */
public interface UserConnectionEvent {
    void onUserDisconnectListener(User user);
    void onUserConnectListener(User user);
}
