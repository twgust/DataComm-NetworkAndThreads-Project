package client.view;

import client.controller.ClientController;

import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JPanel {
    private JFrame frame;
    private JTextPane textPaneChat;
    private JButton buttonSend;
    private JButton buttonAddImage;
    private JFileChooser fileChooserImage;

    // to update gui
    private ClientController clientController;

    public ClientGUI(ClientController clientController, String frameTitle, int width, int height) {
        this.clientController = clientController;
        setupFrame(frameTitle, width, height);
    }

    /**
     * Server and Client can be considered separate projects,
     * suggestions from IDE regarding duplicate code can be ignored.
     * @param title title of frame
     * @param width width of frame
     * @param height height of frame
     */
    private void setupFrame(String title, int width, int height) {
        frame = new JFrame(title);
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
