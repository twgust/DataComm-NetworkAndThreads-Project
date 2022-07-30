package client.controller;

import entity.User;

import java.util.ArrayList;

public interface IUserConnectionCallback {
    void usersUpdated(ArrayList<User> onlineUserList);
}
