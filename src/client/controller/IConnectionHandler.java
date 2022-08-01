package client.controller;

import entity.User;

import java.util.ArrayList;

/**
 *
 */
public interface IConnectionHandler {
    /**
     *
     */
    void usersUpdatedCallback(ArrayList<User> onlineUserList);
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
