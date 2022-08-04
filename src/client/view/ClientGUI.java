package client.view;

import client.controller.ClientController;
import client.controller.IConnectionHandler;
import client.controller.IMessageReceivedHandler;
import entity.Message;
import entity.MessageType;
import entity.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 */
public class ClientGUI implements IConnectionHandler, IMessageReceivedHandler {
    // Swing components
    private JFrame frame;
    private DefaultListModel<User> listModel;
    private JList jlistOnline;
    private JTextArea textAreaChat;
    private JTextField textFieldInput;
    private JButton sendButton;
    private JLabel lblIcon;


    // to update gui
    private final ClientController clientController;

    /**
     *
     */
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
    public void connect(String username, String path){
        clientController.connectToServer(username, path);
    }

    /**
     * Attempts to disconnect the client from the server
     */
    public void disconnect(){
        System.out.println("calling disconnect in gui");
        clientController.disconnectFromServer();
    }

    /**
     * WIP
     * @param message string fetched from textfield
     * @param msgType hardcoded for testing, solving in gui later
     */
    public void sendMessage(String message, MessageType msgType){
        // recipients of message, selected by user in gui

        //test case, send to all connected clients
        ArrayList<User> userArrayList = new ArrayList<>();
        Object[] objects = listModel.toArray();
        // the type of message, also selected in gui
        MessageType type = msgType;
        switch (type){
            case TEXT -> clientController.sendChatMsg(message, objects,  msgType);
        }

    }



    /**
     *
     */
    private void loadImg(){
        clientController.loadImgFromPath("/images/circle_of_fifths.jpg");
    }

    /**
     * Fires after a successful attempt to connect the client to the server
     * @param connected info passed from controller
     */
    @Override
    public void connectionOpenedCallback(String connected, User u) {
        System.out.println(u.toString() + connected );
        SwingUtilities.invokeLater(()->{
            frame.setTitle("signed in as " + u);
        });
    }

    /**
     * fires after a successful attempt to disconnect the client from the server
     * @param disconnected info passed from controller
     */
    @Override
    public void connectionClosedCallback(String disconnected) {
        SwingUtilities.invokeLater(()->{
            textAreaChat.setText("");
            frame.setTitle("offline...");
            listModel.clear();
        });
    }

    /**
     * fires after a user has connected or disconnected
     * @param set  updated list of currently online users
     */
    @Override
    public void usersUpdatedCallback(HashSet<User> set) {
        SwingUtilities.invokeLater(()->{
            set.parallelStream().forEach(user -> {
                if(!listModel.contains(user)){
                    listModel.addElement(user);
                }
            });
        });

    }

    /**
     * @param message fires when message.getType() returns TEXT
     */
    @Override
    public void textMessageReceived(Message message, LocalTime timeNow) {
        SwingUtilities.invokeLater(()->{
            textAreaChat.append(message.getAuthor().toString() + ": " + message.getTextMessage());
        });
    }
    /**
     * @param message fires when message.getType() returns Image
     */
    @Override
    public void imageMessageReceived(Message message, LocalTime timeNow) {

    }
    /**
     * @param message fires when message.getType() returns TEXT_IMAGE
     */
    @Override
    public void txtAndImgMessageReceived(Message message, LocalTime timeNow) {

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
            listModel = new DefaultListModel();
            jlistOnline = new JList(listModel);

            textAreaChat = new JTextArea(5, 5);
            textFieldInput = new JTextField(5);
            sendButton = new JButton("Send");
            lblIcon = new JLabel();

            //adjust size and set layout
            panel.setPreferredSize(new Dimension(557, 464));
            panel.setLayout(null);

            //add components
            panel.add(jlistOnline);
            panel.add(textAreaChat);
            panel.add(textFieldInput);
            panel.add(sendButton);
            panel.add(lblIcon);

            //set component bounds (only needed by Absolute Positioning)
            jlistOnline.setBounds(480, 0, 75, 410);
            textAreaChat.setBounds(0, 0, 475, 410);
            textFieldInput.setBounds(0, 415, 385, 35);
            sendButton.setBounds(385, 415, 90, 35);
            lblIcon.setBounds(475, 415, 35,35);
            setupFrame(panel);
        });
    }
}
