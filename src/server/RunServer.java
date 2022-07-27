package server;

import server.controller.ServerController;

public class RunServer {
    public static void main(String[] args) {
        ServerController serverController = new ServerController(7117);
        serverController.startServer();
    }
}
