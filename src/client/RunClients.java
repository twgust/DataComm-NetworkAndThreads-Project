package client;

import client.view.ClientView;

public class RunClients {
    public static void main(String[] args) {
        ClientView clientView = new ClientView(new Client(), "title", 250, 250);
    }
}
