package server;

import client.controller.ClientController;
import server.controller.ServerController;

public class RunServer {
    public static void main(String[] args) {
        ServerController serverController = new ServerController(9301);
        serverController.startServer();

        ClientController clientController1 = new ClientController("127.0.0.1", 9301);
        ClientController clientController2 = new ClientController("127.0.0.1", 9301);
        clientController1.connectToServer("userOne");
        clientController2.connectToServer("userTwo");
    }
}
