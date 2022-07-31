package client.controller;

import entity.User;

import java.util.ArrayList;

public interface IUserConnectionCallback {
    void usersUpdatedCallback(ArrayList<User> onlineUserList);
    void connectionOpenedCallback(String connected);
    void connectionClosedCallback(String disconnected);
    void exceptionCallback(Exception e, String errorMessage);
}
