package server;

/**
 * Notes and Requirements for Server:
 *
 * Communication Protocol: TCP
 * Communication via: ObjectInputStream and ObjectOutputStream
 *
 * ----------------------------------------------------------------------------*
 * NonFunctional (qualitative) Requirements to implement for Server:
 *
 * 1, Handle a large amount of clients
 * ----------------------------------------------------------------------------*
 *
 * ----------------------------------------------------------------------------*
 * Functionality to implement for Server:
 *
 * 1, Allow for a Client to connect to the server []
 * 2, Allow for a Client to disconnect from the server []
 *
 *
 * 3, Keep an updated List of currently connected Clients in some Data Structure []
 * 3.1 Operations on the Data Structure need be Synchronized
 *
 *
 * 4, Store a Messages that can't be sent in some Data Structure []
 * 4.1 Operations on the Data Structure need be Syncrhonized
 * 4.2 On Client reconnect, send the stored Message []
 *
 * 5, Log all traffic on the server to a locally stored File []
 * 5.1 Display all traffic on the server through a Server GUI (ServerView) []
 * ----------------------------------------------------------------------------*
 *
 *
 */
public class Server {
}
