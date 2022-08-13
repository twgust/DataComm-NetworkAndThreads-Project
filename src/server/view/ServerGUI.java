package server.view;

import server.ServerInterface.LoggerCallBack;
import server.controller.ServerController;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalTime;
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
    private StringBuilder builder;

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
        serverController.sendLoggerCallbackImpl(this);
        builder = new StringBuilder();
    }
    public void init(){
        serverController.startServer();
    }


    /**
     * Server and Client can be considered separate projects,
     * suggestions from IDE regarding duplicate code can be ignored.
     * @param width width
     * @param height height
     */
    private void setupFrame(int width, int height) throws BadLocationException {
        UIDefaults uiDefaults = UIManager.getDefaults();
        uiDefaults.put("activeCaption", new javax.swing.plaf.ColorUIResource(Color.gray));
        uiDefaults.put("activeCaptionText", new javax.swing.plaf.ColorUIResource(Color.white));
        JFrame.setDefaultLookAndFeelDecorated(true);

        frame = new JFrame();
        int minWidth = 850;
        int minHeight = 500;
        if(width < minWidth){
            width = minWidth;
            if(height < minHeight){
                height = minHeight;
            }
        }
        textPane = new JTextPane();
        textPane.setBackground(Color.BLACK);
        set = new SimpleAttributeSet();
        StyleConstants.setItalic(set, true);
        textPane.setCharacterAttributes(set, true);
        doc = (StyledDocument) textPane.getDocument();
        style = doc.addStyle("StyleName", null);
        StyleConstants.setForeground(style, Color.CYAN);
        StyleConstants.setBold(style, true);

        textPane.setBorder(BorderFactory.createMatteBorder(2,2,2,2,Color.CYAN));
        JScrollPane scrollPane1 = new JScrollPane(textPane);
        scrollPane1.setForeground(Color.CYAN);
        scrollPane1.setBorder(BorderFactory.createMatteBorder(3,3,3,3,Color.GREEN));
        frame.add(scrollPane1);
        // textArea = new JTextArea();
        frame.setLocation(750,400);
        frame.setSize(1280, 720);
        frame.setResizable(false);
        frame.setVisible(true);
        // frame.add(textArea);
    }
    private String buildString(Level level, String info, LocalTime time){
        builder = new StringBuilder();
        return builder.append("SERVER: [").append(time.getHour()).append(':')
                .append(time.getMinute()).append(':')
                .append(time.getSecond()).append(']')
                .append('<').append(level.getName()).append('>')
                .append(info).append("\n\n").toString();
    }
    /**
     * Callback interface
     * fires when controller invokes this implementation of logInfoToGui
     * @param level Logger.Level type of event (WARN,INFO etc)
     * @param info string to be logged
     */
    @Override
    public void logInfoToGui(String info, Level level) {
            SwingUtilities.invokeLater(()-> {
                try {
                if (level.equals(Level.WARNING)) {
                    doc.insertString(doc.getLength(), info, style);
                }
                else {
                    doc.insertString(doc.getLength(), info, style);
                }

                }
                catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(frame, "BadLocationException E\n" + info);
                }
            });
    }
}
