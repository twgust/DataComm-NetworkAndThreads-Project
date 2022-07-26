package entity;

/**
 * MESSAGE FROM CLIENT:
 *
 * A Message should consist of one of the following components:
 * Text OR
 * Image OR
 * Text AND Image
 *
 * Image file format can be one of the following:
 * .JPG OR
 * .PNG
 *
 * Relations:
 * Message HAS A User (author of message)
 * Message HAS A List of Users (recipients of message)
 *
 *
 * Extra:
 * TimeDate when Message was received by server (this is handled by server)
 * TimeDate when Message was received by client (see above)
 */
public class Message {
}
