package server;
import server.Controller.ServerController;
import server.view.ServerGUI;

import javax.swing.text.BadLocationException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class RunServer {
    public static void main(String[] args) throws BadLocationException, IOException {
        try {
            String path = getProgramPath2();

            String fileSeparator = System.getProperty("file.separator");
            String newDir = path + fileSeparator + "User Avatars" + fileSeparator;

            File file = new File(newDir);
            file.mkdir();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File directory = new File("images");
        if (! directory.exists()){
            directory.mkdir();
        }
        ServerController serverController = new ServerController();
        ServerGUI serverGUI = new ServerGUI(serverController,500, 500);
        // maybe read from cli and start server that way, give the host option to choose port etc
        serverGUI.init();
    }
    // maybe read from cli and start server that way, give the host option to choose port etc
    public static String getProgramPath2() throws UnsupportedEncodingException {
        URL url = RunServer.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = URLDecoder.decode(url.getFile(), "UTF-8");
        String parentPath = new File(jarPath).getParentFile().getPath();
        return parentPath;
    }
}
