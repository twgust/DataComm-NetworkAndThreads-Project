package client.controller;

import entity.User;

import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 */
public interface IConnectionHandler {
    /**
     *
     */
    void usersUpdatedCallback(HashSet<User> onlineUserSet);
    /**
     *
     */
    void connectionOpenedCallback(String connected, User u);
    /**
     *
     */
    void connectionClosedCallback(String disconnected);
    /**
     *
     */
    void exceptionCallback(Exception e, String errorMessage);
}
