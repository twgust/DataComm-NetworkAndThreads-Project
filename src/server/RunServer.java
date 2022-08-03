package server;
import server.controller.ServerController;
import server.view.ServerGUI;

import javax.swing.text.BadLocationException;

public class RunServer {
    public static void main(String[] args) throws BadLocationException {
        ServerController serverController = new ServerController(9301);
        ServerGUI serverGUI = new ServerGUI(serverController,500, 500);
    }
}
