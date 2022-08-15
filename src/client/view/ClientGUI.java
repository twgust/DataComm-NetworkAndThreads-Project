package client.view;

import client.controller.ClientController;
import client.controller.IConnectionHandler;
import client.controller.IMessageReceivedHandler;
import entity.Message;
import entity.MessageType;
import entity.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 */
public class ClientGUI implements IConnectionHandler, IMessageReceivedHandler {
    // Swing components
    private JFrame frame;
    private DefaultListModel<User> onlineListModel;
    private DefaultListModel<User> contactListModel;
    private JList<User> jlistOnline;
    private JList<User> jListContacts;
    private JTextArea textAreaChat;
    private JTextField textFieldInput;
    private JButton sendButton;
    private JLabel lblIcon;
    private JTabbedPane tabbedPane;
    private JFileChooser fileChooser;
    private JButton attachFileButton;
    private JButton addToContactsButton;
    private JButton removeContactButton;
    private JPanel onlineListPanel;
    private JPanel contactListPanel;
    private JPanel chatPanel;
    private File selectedFile = null;


    // to update gui
    private final ClientController clientController;

    /**
     *
     */
    public ClientGUI(ClientController clientController) {
        this.clientController = clientController;
        this.clientController.addConnectionHandler(this);
        this.clientController.addMessageReceivedHandler(this);
        setupFrame();
        createListeners();
    }

    /**
     * Attempts to connect the user to the server
     * @author Alexandra Koch
     */
    public void connect(){
        InetSocketAddress socketAddress = getIPCallback();
        while (socketAddress == null) {
            socketAddress = getIPCallback();
        }
        String username = JOptionPane.showInputDialog("Input username");
        String path;
        JFileChooser avatarChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Images, jpg, png", "jpg","png");
        avatarChooser.setFileFilter(filter);
        if (fileChooser.showOpenDialog(chatPanel) == JFileChooser.APPROVE_OPTION) {
            path = fileChooser.getSelectedFile().getPath();
        }
        else {
            path = "";
        }

        clientController.connectToServer(username, path, socketAddress);
    }

    /**
     * Attempts to disconnect the client from the server
     */
    public void disconnect(){
        System.out.println("calling disconnect in gui");
        clientController.disconnectFromServer();

    }

    /**
     * GUI invokes
     * @param message string fetched from textfield
     * @param msgType hardcoded for testing, solving in gui later
     */
    public void sendMessage(String message,String path ,MessageType msgType, Object[] recipients ){
        if(onlineListModel.isEmpty()){
            System.out.println("U haven't selected any recipients!");
            return;
        }

        // recipients of message, selected by user in gui
        SwingUtilities.invokeLater(()->{
            //test case, send to all connected clients
            // the type of message, also selected in gui
            MessageType type = msgType;
            switch (type){
                case TEXT -> {
                    try {
                        clientController.sendChatMsg(message, recipients,  msgType);
                    } catch (IOException e) { e.printStackTrace(); }
                }
                case IMAGE -> {
                    try {
                        clientController.sendChatMsg(recipients,msgType,path);
                    } catch (IOException e) { e.printStackTrace(); }
                }
                case TEXT_IMAGE -> {
                    try {
                        clientController.sendChatMsg(message,recipients,msgType, path);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }});
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
            onlineListModel.clear();
        });
    }

    /**
     * fires after a user has connected or disconnected
     * @param set  updated list of currently online users
     */
    @Override
    public void usersUpdatedCallback(HashSet<User> set) {
        System.out.println("Asdasd");
        System.out.println(set.size() + " " + clientController.getLocalPort());
        SwingUtilities.invokeLater(()->{
            set.forEach(user -> {
                onlineListModel.clear();
                onlineListModel.addAll(set);
            });
        });
    }
    @Override
    public void contactsUpdatedCallback(HashSet<User> set) {
        contactListModel.clear();
        contactListModel.addAll(set);
    }

    /**
     * @param message fires when message.getType() returns TEXT
     */
    @Override
    public void textMessageReceived(Message message, LocalTime timeNow) {
        SwingUtilities.invokeLater(()->{
            System.out.println(message.getTextMessage());
            textAreaChat.append(timeNow.now().getHour() +":" + timeNow.now().getMinute()+ ":" + timeNow.now().getSecond()+ " >" + message.getAuthor().getUsername() + ": " + message.getTextMessage() + " [" + message.getType() +"]\n");        });
    }
    /**
     * @param message fires when message.getType() returns Image
     */
    @Override
    public void imageMessageReceived(Message message, LocalTime timeNow) {
        SwingUtilities.invokeLater(() -> {
            ImageIcon img = byteArrToImageIcon(message.getImage());
            User author = message.getAuthor();
            textAreaChat.append(timeNow.now().getHour() + ":" + timeNow.now().getMinute() + ":" + timeNow.now().getSecond() +
                    " >" + message.getAuthor().getUsername() + ": " + "Image message" +
                    " [" + message.getType() + ", Size=" + message.getImage().length + " bytes]\n");
            JOptionPane.showMessageDialog(null, img);
        });
    }

    /**
     * @param message fires when message.getType() returns TEXT_IMAGE
     */
    @Override
    public void txtAndImgMessageReceived(Message message, LocalTime timeNow) {
        SwingUtilities.invokeLater(()->{
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
            ImageIcon img = byteArrToImageIcon(message.getImage());
            textAreaChat.append(timeNow.now().getHour() +":" + timeNow.now().getMinute()+ ":" + timeNow.now().getSecond()+
                    " >" + message.getAuthor().getUsername() + ": " + message.getTextMessage() + "" +
                    " [" + message.getType() +  ", Size="+ message.getImage().length+" bytes]\n");

            String text = message.getTextMessage();
            User author = message.getAuthor();
            JOptionPane.showMessageDialog(null, img);

        });
    }

    /**
     * @return returns an InetSocketAddress object
     * @author Alexandra Koch
     */
    @Override
    public InetSocketAddress getIPCallback() {
        InetAddress ip;
        InetSocketAddress socketAddress = null;
        try {
            ip = InetAddress.getByName(JOptionPane.showInputDialog("Enter IP", "127.0.0.1"));
        } catch (UnknownHostException e) {
            return null;

        }
        int port = Integer.parseInt(JOptionPane.showInputDialog("Enter port", "42652"));
        try {
            socketAddress = new InetSocketAddress(ip, port);
        } catch (Exception e) {
            return null;
        }
        return socketAddress;
    }
    private synchronized ImageIcon byteArrToImageIcon(byte[] img){

        try{
            ByteArrayInputStream bais = new ByteArrayInputStream(img);
            BufferedImage bufferedImage = ImageIO.read(bais);
            return new ImageIcon(bufferedImage);

        }catch (IOException e){
            e.printStackTrace();
        }

        return null;
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
     * Sets up JFrame for GUI
     * @author twgust, Alexandra Koch
     */
    private void setupFrame() {
        setupChatPanel();
        setupListPanel();
        frame = new JFrame("chat client");
        //frame.setLayout();
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        frame.setPreferredSize(new Dimension(735, 464));
        frame.getContentPane().setPreferredSize(new Dimension(735 ,464));
        frame.getContentPane().add(chatPanel);
        frame.getContentPane().add(Box.createRigidArea(new Dimension(15,0)));
        frame.getContentPane().add(tabbedPane);
        frame.getContentPane().add(Box.createRigidArea(new Dimension(15,0)));
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }
    /**
     * Sets up chat panel, with text entry and display fields + associated buttons
     * @author twgust, Alexandra Koch
     */
    private void setupChatPanel() {
            chatPanel = new JPanel();
            //construct components

            textAreaChat = new JTextArea(5, 5);
            textFieldInput = new JTextField(5);
            attachFileButton = new JButton("Attach image");
            sendButton = new JButton("Send");
            lblIcon = new JLabel();
            fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Images, jpg, png", "jpg","png");
            fileChooser.setFileFilter(filter);

            //adjust size and set layout
            chatPanel.setPreferredSize(new Dimension(475, 464));
            chatPanel.setLayout(null);

            //add components
            chatPanel.add(textAreaChat);
            chatPanel.add(attachFileButton);
            chatPanel.add(textFieldInput);
            chatPanel.add(sendButton);
            chatPanel.add(lblIcon);

            //set component bounds (only needed by Absolute Positioning)
            textAreaChat.setBounds(0, 0, 475, 410);
            attachFileButton.setBounds(0,415,90,35);
            textFieldInput.setBounds(90, 415, 295, 35);
            sendButton.setBounds(385, 415, 90, 35);
            lblIcon.setBounds(475, 415, 35,35);
    }
    /**
     * Sets up contact list panel
     * @author Alexandra Koch
     */
    private void setupListPanel() {
            tabbedPane = new JTabbedPane();
            tabbedPane.setPreferredSize(new Dimension(230, 410));

            //construct JPanel for online users list

            onlineListPanel = new JPanel();

            onlineListModel = new DefaultListModel<>();
            jlistOnline = new JList<>(onlineListModel);

            addToContactsButton = new JButton("Add to contacts");

            onlineListPanel.setPreferredSize(new Dimension(230, 410));
            onlineListPanel.setLayout(null);

            jlistOnline.setBounds(0, 0, 230, 380);
            addToContactsButton.setBounds(0, 380, 230, 30 );
            onlineListPanel.add(jlistOnline);
            onlineListPanel.add(addToContactsButton);
            tabbedPane.addTab("Online", onlineListPanel);

            //construct JPanel for contacts list

            contactListPanel = new JPanel();

            contactListModel = new DefaultListModel<>();
            jListContacts = new JList<>(contactListModel);

            removeContactButton = new JButton("Remove from contacts");

            contactListPanel.setPreferredSize(new Dimension(230, 410));
            contactListPanel.setLayout(null);

            removeContactButton.setBounds(0, 380, 230, 30);
            jListContacts.setBounds(0, 0, 230, 380);
            contactListPanel.add(jListContacts);
            contactListPanel.add(removeContactButton);
            tabbedPane.addTab("Contacts", contactListPanel);

    }
    /**
     * Sets up event listeners for GUI
     * @author Alexandra Koch
     */
    private void createListeners() {

        addToContactsButton.addActionListener(actionEvent -> {
            clientController.addContact(jlistOnline.getSelectedValue());
        });

        removeContactButton.addActionListener(actionEvent -> {
            clientController.removeContact(jListContacts.getSelectedValue());
        });
        sendButton.addActionListener(actionEvent -> {
            int selectedIndex;
            Component selectedTab = tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
            Object prelimJList = selectedTab.getComponentAt(0,0);
            JList confirmedJList;
            if (prelimJList instanceof JList) {
                confirmedJList = (JList) prelimJList;
            }
            else {
                System.out.println("Unexpected error in finding JList");
                return;
            }
            //text only message
            if (selectedFile == null && !Objects.equals(textFieldInput.getText(), "")) {
                sendMessage(textFieldInput.getText(),"",MessageType.TEXT,confirmedJList.getSelectedValuesList().toArray());
                textFieldInput.setText("");
            }
            //text+image message
            else if (selectedFile != null && !Objects.equals(textFieldInput.getText(), "")) {
                sendMessage(textFieldInput.getText(), selectedFile.getAbsolutePath(), MessageType.TEXT_IMAGE, confirmedJList.getSelectedValuesList().toArray());
                textFieldInput.setText("");
                selectedFile = null;
            }
            //image only message
            else if (selectedFile != null){
                sendMessage("Image message", selectedFile.getAbsolutePath(), MessageType.IMAGE, confirmedJList.getSelectedValuesList().toArray());
                selectedFile = null;
            }
        });

        //fires when attachment button is pressed
        attachFileButton.addActionListener(actionEvent -> {
            if (fileChooser.showOpenDialog(chatPanel) == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
            };
        });

        //mouse-listeners for right-click avatar display
        jlistOnline.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int clickedIndex = jlistOnline.locationToIndex(e.getPoint());
                    avatarPopup(onlineListModel.getElementAt(clickedIndex));
                }
            }
        });
        jListContacts.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int clickedIndex = jListContacts.locationToIndex(e.getPoint());
                    avatarPopup(contactListModel.getElementAt(clickedIndex));
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
                clientController.writeContactFile();
            }
        });
    }
    /**
     * displays a Users avatar in a message dialog
     * fires when a user is right-clicked in JList object
     * @param user  A user object
     * @author Alexandra Koch
     */
    private void avatarPopup(User user) {
        JOptionPane.showMessageDialog(null,  byteArrToImageIcon(user.getAvatarAsByteBuffer()));
    }

    public User getUser() {
        return clientController.getUser();
    }
}
