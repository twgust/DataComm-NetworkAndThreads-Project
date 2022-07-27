package client;

import client.controller.ClientController;
import client.view.ClientGUI;

public class RunClient {
    public static void main(String[] args) {
        ClientController clientController = new ClientController();
        ClientGUI clientView = new ClientGUI(clientController, "title", 250, 250);
    }
}
