package client;

import client.controller.ClientController;
import client.view.ClientGUI;

public class RunClient {
    public static void main(String[] args) {
        ClientController clientController1 = new ClientController("127.0.0.1", 9301);
        ClientController clientController2 = new ClientController("127.0.0.1", 9301);
        clientController1.connectToServer();
        clientController2.connectToServer();
    }
}
