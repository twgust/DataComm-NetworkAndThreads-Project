package server;
import server.controller.ServerController;
import server.view.ServerGUI;

import javax.swing.text.BadLocationException;
import java.io.IOException;

public class RunServer {
    public static void main(String[] args) throws BadLocationException, IOException {
        ServerController serverController = new ServerController();
        ServerGUI serverGUI = new ServerGUI(serverController,500, 500);
        // maybe read from cli and start server that way, give the host option to choose port etc
        serverGUI.init();
    }
}
