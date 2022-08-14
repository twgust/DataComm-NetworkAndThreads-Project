package client.controller;

import entity.User;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;

public class ContactListFileHandler {
    File directory;
    File contactsFile;
    public ContactListFileHandler(User user) {
        directory = new File("userdata");
        contactsFile = new File("userdata/"+user.getUsername()+".contacts");
        if (! directory.exists()) {
            try {
                directory.mkdir();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    /**
     *Reads contacts from a file
     * @return A HashSet of Users read from the contacts file
     * @author Alexandra Koch
     */
    public HashSet<User> readContactFile() {
        if (contactsFile.exists()) {
                    try (FileInputStream inputStream = new FileInputStream(contactsFile)) {
                        ObjectInputStream ois = new ObjectInputStream(inputStream);
                        return (HashSet<User>) ois.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            return null;
        }
    /**
     *Writes contacts to a file
     * @param userSet A HashSet of Users to write
     * @author Alexandra Koch
     */
        public void writeContactFile(HashSet<User> userSet) {
            if (contactsFile.exists()) {
                try {
                    contactsFile.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            try (FileOutputStream outputStream = new FileOutputStream(contactsFile)) {
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(userSet);
                oos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

}




