package entity;

import java.io.Serializable;
/**
 * @author twgust
 */
public abstract class Sendables implements Serializable {
    private SendableType sendableType;

    public abstract SendableType getSendableType();
}
