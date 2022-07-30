package server;
import client.controller.ClientController;
import client.view.ClientGUI;
import server.controller.ServerController;
import server.view.ServerGUI;

public class runLocalServerAndClients {
    public static void main(String[] args) {
        ServerController serverController = new ServerController(9301);
        ServerGUI serverGUI = new ServerGUI(serverController,500, 500);
        serverController.startServer();

        // some sleeping to simulate real behavior
        try{
            System.out.println("MAIN - server instantiated, going to sleep for 2 seconds");
            Thread.sleep(2000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("MAIN - Starting clients");
        // distinguish between local clients by port number, not remote address
        // gui starts connection to server because a user will click connect
        ClientController clientController1 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui1 = new ClientGUI(clientController1);
        gui1.connect("userOne");

        ClientController clientController2 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui2 = new ClientGUI(clientController2);
        gui2.connect("userTwo");

        ClientController clientController3 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui3 = new ClientGUI(clientController3);
        gui3.connect("userThree");

    }
}
