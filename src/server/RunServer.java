package server;

import client.controller.ClientController;
import server.controller.ServerController;

public class RunServer {
    public static void main(String[] args) {
        ServerController serverController = new ServerController(9301);
        serverController.startServer();
    }
}
