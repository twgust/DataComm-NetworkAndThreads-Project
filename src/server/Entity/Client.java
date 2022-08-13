package server.Entity;

import entity.User;
import server.controller.Buffer.MessageBuffer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Representation of a socket + the associated user
 * @author twgust
 */
public class Client {


    public User getUser() {
        return user;
    }

    private User user;
    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    public ObjectInputStream getOis() {
        return ois;
    }

    public Client(User user,Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.user = user;
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;

    }

    private final Socket socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;
}
