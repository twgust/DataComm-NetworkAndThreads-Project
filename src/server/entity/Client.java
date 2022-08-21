package server.entity;

import entity.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Representation of a socket + the associated user
 * @author twgust
 */
public class Client {
    private final User user;
    private final Socket socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream ois;


    public Client(User user,Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.user = user;
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;

    }

    public User getUser() {
        return user;
    }
    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getOos() {
        return oos;
    }

    public ObjectInputStream getOis() {
        return ois;
    }
}
