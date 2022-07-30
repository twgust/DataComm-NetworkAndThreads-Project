package client.view;

import client.controller.ClientController;
import client.controller.IUserConnectionCallback;
import entity.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ClientGUI implements IUserConnectionCallback {
    private JFrame frame;
    private JList jlistOnline;
    private JTextArea textAreaChat;
    private JTextField textFieldInput;
    private JButton sendButton;

    // to update gui
    private ClientController clientController;

    public ClientGUI(ClientController clientController) {
        this.clientController = clientController;
        this.clientController.addCallBackListener(this);
        setupPanel();
    }
    public void connect(String username){
        clientController.connectToServer(username);
    }

    @Override
    public void usersUpdated(ArrayList<User> onlineUserList) {
        // System.out.println("time to update gui, client:" + clientController.toString());
        // System.out.println(onlineUserList.size());
        System.out.println("callback interface fired, updating gui");
        SwingUtilities.invokeLater(() -> {
            textAreaChat.append(onlineUserList.size() + "\n");
        });

    }

    /**
     * Server and Client can be considered separate projects,
     * suggestions from IDE regarding duplicate code can be ignored.
     */
    private void setupFrame(JPanel panel) {
        frame = new JFrame("chat client");
        frame.getContentPane().add(panel);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }

    private void setupPanel() {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel();
            //construct components
            jlistOnline = new JList();
            textAreaChat = new JTextArea(5, 5);
            textFieldInput = new JTextField(5);
            sendButton = new JButton("Send");

            //adjust size and set layout
            panel.setPreferredSize(new Dimension(557, 464));
            panel.setLayout(null);

            //add components
            panel.add(jlistOnline);
            panel.add(textAreaChat);
            panel.add(textFieldInput);
            panel.add(sendButton);

            //set component bounds (only needed by Absolute Positioning)
            jlistOnline.setBounds(480, 0, 75, 410);
            textAreaChat.setBounds(0, 0, 475, 410);
            textFieldInput.setBounds(0, 415, 385, 35);
            sendButton.setBounds(385, 415, 90, 35);
            setupFrame(panel);
        });
    }


}
