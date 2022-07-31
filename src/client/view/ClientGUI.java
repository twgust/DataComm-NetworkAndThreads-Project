package client.view;

import client.controller.ClientController;
import client.controller.IConnectionHandler;
import client.controller.IMessageReceivedHandler;
import entity.Message;
import entity.User;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ClientGUI implements IConnectionHandler, IMessageReceivedHandler {
    // Swing components
    private JFrame frame;
    private JList jlistOnline;
    private JTextArea textAreaChat;
    private JTextField textFieldInput;
    private JButton sendButton;

    // to update gui
    private final ClientController clientController;

    public ClientGUI(ClientController clientController) {
        this.clientController = clientController;
        this.clientController.addConnectionHandler(this);
        this.clientController.addMessageReceivedHandler(this);
        setupPanel();
    }

    /**
     * Attempts to connect the user to the server
     * @param username desired username
     */
    public void connect(String username, ImageIcon icon){
        clientController.connectToServer(username);
    }

    /**
     * Attempts to disconnect the client from the server
     */
    public void disconnect(){
        clientController.disconnectFromServer();
    }

    /**
     * Fires after a successful attempt to connect the client to the server
     * @param connected info passed from controller
     */
    @Override
    public void connectionOpenedCallback(String connected) {

    }

    /**
     * fires after a successful attempt to disconnect the client from the server
     * @param disconnected info passed from controller
     */
    @Override
    public void connectionClosedCallback(String disconnected) {

    }

    /**
     * fires after a user has connected or disconnected
     * @param onlineUserList updated list of currently online users
     */
    @Override
    public void usersUpdatedCallback(ArrayList<User> onlineUserList) {
        SwingUtilities.invokeLater(() -> {
            textAreaChat.append(onlineUserList.size() + "\n");
        });
    }

    /**
     * @param message fires when message.getType() returns TEXT
     */
    @Override
    public void textMessageReceived(Message message) {

    }
    /**
     * @param message fires when message.getType() returns Image
     */
    @Override
    public void imageMessageReceived(Message message) {

    }
    /**
     * @param message fires when message.getType() returns TEXT_IMAGE
     */
    @Override
    public void txtAndImgMessageReceived(Message message) {

    }

    /**
     * OPTIONAL
     * fires after on exception in client controller
     * optional displaying of error message to user
     * @param e the exception which occurred
     * @param errorMessage error message for client
     */
    @Override
    public void exceptionCallback(Exception e, String errorMessage) {
        System.out.println(e.getMessage());
    }
    /**
     * Mock gui for testing
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
