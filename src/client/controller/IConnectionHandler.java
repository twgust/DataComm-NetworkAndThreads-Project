package client.controller;

import entity.User;

import java.net.InetSocketAddress;
import java.util.HashSet;

/**
 * @author twgust
 */
public interface IConnectionHandler {
    /**
     *
     */
    void usersUpdatedCallback(HashSet<User> onlineUserSet);
    /**
     *
     */
    void contactsUpdatedCallback(HashSet<User> contactSet);
    void connectionOpenedCallback(String connected, User u);
    /**
     *
     */
    void connectionClosedCallback(String disconnected);
    /**
     *
     */
    void exceptionCallback(Exception e, String errorMessage);

    InetSocketAddress getIPCallback();
}
