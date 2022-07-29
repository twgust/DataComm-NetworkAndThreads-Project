package server;
import client.controller.ClientController;
import server.controller.ServerController;
import server.view.ServerGUI;

public class runLocalServerAndClients {
    public static void main(String[] args) {
        ServerController serverController = new ServerController(9301);
        ServerGUI serverGUI = new ServerGUI(serverController,500, 500);
        serverController.startServer();

        // distinguish between local clients by port number, not remote address
        ClientController clientController1 = new ClientController("127.0.0.1", 9301);
        ClientController clientController2 = new ClientController("127.0.0.1", 9301);
        clientController1.connectToServer();
        clientController2.connectToServer();
        clientController1.registerUser("MyUserName");
        clientController2.registerUser("AnotherUserName");

    }
}
