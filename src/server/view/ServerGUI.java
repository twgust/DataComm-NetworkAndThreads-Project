package server.view;

import entity.User;
import server.controller.LoggerCallBack;
import server.controller.ServerController;
import server.controller.UserConnectionCallback;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.logging.Level;

public class ServerGUI implements LoggerCallBack {
    private ServerController serverController;
    private JTextArea textArea;

    private JTextPane textPane;
    private SimpleAttributeSet set;
    private StyledDocument doc;
    private JScrollPane scrollPane;
    private JFrame frame;
    private Style style;

    private DefaultListModel listModel;
    private JList list;

    private Font font;

    /**
     * Constructor
     * @param serverController reference for interfacing
     * @param width width of Frame
     * @param height height of Frame
     */
    public ServerGUI(ServerController serverController, int width, int height) throws BadLocationException {
        setupFrame(width,height);
        this.serverController = serverController;
        //serverController.addConnectionListener(this);
        serverController.addLoggerCallbackImpl(this);
    }


    /**
     * Server and Client can be considered separate projects,
     * suggestions from IDE regarding duplicate code can be ignored.
     * @param width width
     * @param height height
     */
    private void setupFrame(int width, int height) throws BadLocationException {
        frame = new JFrame();
        int minWidth = 500;
        int minHeight = 500;
        if(width < minWidth){
            width = minWidth;
            if(height < minHeight){
                height = minHeight;
            }
        }
        //Todo impl
        Container container = frame.getContentPane();
        textPane = new JTextPane();

        textPane.setBackground(Color.black);
        set = new SimpleAttributeSet();
        StyleConstants.setItalic(set, true);
        textPane.setCharacterAttributes(set, true);

        Font font = new Font("Verdana", Font.BOLD, 12);
        textPane.setFont(font);

        doc = (StyledDocument) textPane.getDocument();
        style = doc.addStyle("StyleName", null);
        StyleConstants.setForeground(style, Color.CYAN);
        StyleConstants.setBold(style, true);
        scrollPane = new JScrollPane(textPane);


    //    textArea = new JTextArea();
        frame.setSize(new Dimension(width,height));
        frame.setResizable(false);
        frame.setVisible(true);
       // frame.add(textArea);
        frame.add(textPane);
    }

    /**
     * Callback interface
     * fires when controller invokes this implementation of logInfoToGui
     * @param level Logger.Level type of event (WARN,INFO etc)
     * @param color To be implemented
     * @param info
     */
    @Override
    public void logInfoToGui(Level level, String color, String info) {
            SwingUtilities.invokeLater(()-> {
                if (level.equals(Level.WARNING)) {
                    // change style
                }
                else {
                    try {
                        doc.insertString(doc.getLength(), level.getName() + ": " + info + "\n\n", style);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            });


    }
}
