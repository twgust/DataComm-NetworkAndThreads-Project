package client;

import client.controller.ClientController;
import client.view.ClientGUI;
import entity.MessageType;

public class RunClient {
    public static void main(String[] args) {
        System.out.println("MAIN - Starting clients");
        // distinguish between local clients by port number, not remote address
        // gui starts connection to server because a user will click connect
        ClientController clientController1 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui1 = new ClientGUI(clientController1);
        gui1.connect("userOne", "src/client/images/circle_of_fifths.jpg");

        ClientController clientController2 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui2 = new ClientGUI(clientController2);
        gui2.connect("userTwo", "src/client/images/music-circle-of-fifths.jpg");

        ClientController clientController3 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui3 = new ClientGUI(clientController3);
        gui3.connect("userThree", "src/client/images/bayerndrei.jpg");

        ClientController clientController4 = new ClientController("127.0.0.1", 9301);
        ClientGUI gui4 = new ClientGUI(clientController4);
        gui4.connect("userFour", "src/client/images/cat.jpg");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //gui1.sendMessage("hello world!",MessageType.TEXT);
    }
}
