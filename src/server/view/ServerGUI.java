package server.view;

import server.controller.ServerController;

import javax.swing.*;
import java.awt.*;

public class ServerGUI {
    private ServerController serverController;
    private JFrame frame;


    public ServerGUI(ServerController serverController, int width, int height){
        this.serverController = serverController;
        setupFrame(width,height);
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
        frame.setSize(new Dimension(width,height));
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
