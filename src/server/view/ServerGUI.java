package server.view;

import entity.User;
import server.controller.ServerController;
import server.controller.UserConnectionCallback;

import javax.swing.*;
import java.awt.*;

public class ServerGUI implements UserConnectionCallback {
    private ServerController serverController;
    private JTextArea textArea;
    private JFrame frame;


    public ServerGUI(ServerController serverController, int width, int height){
        setupFrame(width,height);
        this.serverController = serverController;
        serverController.addConnectionListener(this);
    }

    @Override
    public void onUserDisconnectListener(User user) {

    }

    @Override
    public void onUserConnectListener(User user) {
        logMessageToGui(user.toString() + " connected to server");
    }

    /**
     * Server and Client can be considered separate projects,
     * suggestions from IDE regarding duplicate code can be ignored.
     * @param width width
     * @param height height
     */
    private void setupFrame(int width, int height){
        frame = new JFrame();
        int minWidth = 500;
        int minHeight = 500;
        if(width < minWidth){
            width = minWidth;
            if(height < minHeight){
                height = minHeight;
            }
        }
        textArea = new JTextArea();
        frame.setSize(new Dimension(width,height));
        frame.setResizable(false);
        frame.setVisible(true);
        frame.add(textArea);
    }
    private void logMessageToGui(String message){
        SwingUtilities.invokeLater(()->{
            textArea.append(message + "\n");
        });
    }
}
