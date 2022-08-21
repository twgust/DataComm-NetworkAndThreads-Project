package client;

import client.controller.ClientController;
import client.view.ClientGUI;
import entity.MessageType;
import entity.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunClient {
    public static void main(String[] args) {

        System.out.println("MAIN - Starting clients");
        // distinguish between local clients by port number, not remote address// gui starts connection to server because a user will click connect

        String ip = "127.0.0.1";
        ClientController c1 = new ClientController( "CLIENT-1");
        ClientGUI g1 = new ClientGUI(c1);
        ExecutorService threadpool = Executors.newFixedThreadPool(25);
        threadpool.submit(g1::connect);


    }
}
