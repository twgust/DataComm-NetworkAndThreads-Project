package entity;

import java.io.Serializable;
/**
 * @author twgust
 * Every object which is "sendable", that is to say every object the server will send to the clients, and clients to server
 * inherits from this "Sendables" abstract class.
 * The class implements Serializible and HAS A SendableType.
 */
public abstract class Sendables implements Serializable {
    private SendableType sendableType;

    public abstract SendableType getSendableType();
}
