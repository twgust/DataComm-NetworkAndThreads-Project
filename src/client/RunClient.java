package client;

import client.controller.ClientController;
import client.view.ClientGUI;
import entity.MessageType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunClient {
    public static void main(String[] args)throws InterruptedException {
        System.out.println("MAIN - Starting clients");
        // distinguish between local clients by port number, not remote address
        // gui starts connection to server because a user will click connect

        ExecutorService service = Executors.newFixedThreadPool(4);
        String ip = "127.0.0.1";
        ClientController c1 = new ClientController(ip, 65465, "CLIENT-1");
        ClientController c2 = new ClientController(ip, 65465, "CLIENT-2");
        ClientController c3 = new ClientController(ip, 65465, "CLIENT-3");
        ClientController c4 = new ClientController(ip, 65465, "CLIENT-4");
        ClientController c5 = new ClientController(ip, 65465, "CLIENT-5");
        ClientGUI g1 = new ClientGUI(c1);
        ClientGUI g2 = new ClientGUI(c2);
        ClientGUI g3 = new ClientGUI(c3);
        ClientGUI g4 = new ClientGUI(c4);
        ClientGUI g5 = new ClientGUI(c5);
        g1.connect("user-6", "src/client/images/circle_of_fifths.jpg");
        g2.connect("user-7", "src/client/images/cat.jpg");
        g3.connect("user-8","src/client/images/circle_of_fifths.jpg");
        g4.connect("user-9", "src/client/images/music-circle-of-fifths.jpg");
        g5.connect("user-10","src/client/images/circle_of_fifths.jpg");
        Thread.sleep(2000);
    }
}
