package client.view;

import client.Client;

import javax.swing.*;
import java.awt.*;

public class ClientView extends JPanel {
    private JTextPane textPaneChat;
    private JButton buttonSend;
    private JButton buttonAddImage;
    private JFileChooser fileChooserImage;

    // to update gui
    private Client client;

    public ClientView(Client client, String frameTitle, int width, int height) {
        this.client = client;
        setupGui(frameTitle, width, height);
    }

    private void setupGui(String title, int width, int height) {
        JFrame frame = new JFrame();
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
