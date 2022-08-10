package server.controller.Threads.Sender;

import entity.*;
import server.Entity.Client;
import server.ServerInterface.UserConnectionEvent;
import server.controller.Buffer.ClientBuffer;
import server.controller.Buffer.MessageBuffer;
import server.controller.Buffer.SendablesBuffer;
import server.controller.Buffer.UserBuffer;
import server.controller.ServerLogger;
import server.controller.Threads.Sender.MessageCallable;

import javax.sql.ConnectionEventListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Runnable for sending a message to a Client,
 * Writes to connected clients ObjectOutputStream
 */
public class ObjectSenderThread {
    private final ServerLogger logger;
    private final ClientBuffer clientBuffer;
    private final MessageBuffer messageBuffer;
    private final UserBuffer userBuffer;
    private final SendablesBuffer sendablesBuffer;

    private ThreadPoolExecutor threadPoolExecutor;
    private UserConnectionEvent listener;

    /**
     * Constructor,
     * @param logger
     * @param clientBuffer reference to buffer (final)
     * @param userBuffer   reference to buffer (final)
     */
    public ObjectSenderThread(ServerLogger logger, SendablesBuffer buffer,ClientBuffer clientBuffer, UserBuffer userBuffer, MessageBuffer messageBuffer) {
        this.logger = logger;
        this.messageBuffer = messageBuffer;
        this.clientBuffer = clientBuffer;
        this.userBuffer = userBuffer;
        this.sendablesBuffer = buffer;
    }

    /**
     * Sets executor, invoked by controller
     * @param threadPoolExecutor has to be thread pool
     * @return
     */
    public boolean setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor){
        this.threadPoolExecutor = threadPoolExecutor;
        assert this.threadPoolExecutor != null;
        return true;
    }
    public void setConnectionListener(UserConnectionEvent listener){
        this.listener = listener;
    }
    public synchronized void start()  {
        while (sendablesBuffer != null) {
            try{
                Sendables sendable = sendablesBuffer.dequeueSendable();
                Message message = null;
                UserSet userSet = null;
                switch (sendable.getSendableType()) {
                    case Message -> {
                        message = (Message) sendable;
                        Message finalMessage = message;

                        List<Client> list = new ArrayList<>();
                        message.getRecipientList().forEach(user -> list.add(clientBuffer.get(user)));
                        ArrayList<MessageCallable> callables = new ArrayList<>(list.size());

                        list.forEach(client -> {
                            MessageCallable callable = new MessageCallable(finalMessage, client);
                            callables.add(callable);
                        });
                        try{
                            List<Future<ObjectOutputStream>> resultList = threadPoolExecutor.invokeAll(callables);
                        }catch (Exception e){
                            //listener.onUserDisconnectListener(clientBuffer);
                            // call on user disconnect for controller
                            e.printStackTrace();
                            Thread.sleep(100);
                        }
                    }
                    case UserSet -> {
                        userSet = (UserSet) sendable;
                        UserSet finalSet = userSet;
                        userBuffer.getHashSet().stream().forEach(user -> {
                            Client client = clientBuffer.get(user);
                            threadPoolExecutor.submit(new ListSender(sendable, client));
                        });
                    }

                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }


    /**
     * Runnable to send update UserSet representing a list of online clients
     */
    private class ListSender implements Runnable{
        private Sendables set;
        private Client client;
        public ListSender(Sendables set, Client client){
            this.set = set;
            this.client = client;
        }
        @Override
        public void run() {
            String thread = Thread.currentThread().getName();
            System.out.println("LISTSENDER " + thread);
            try {
                ObjectOutputStream oos;
                oos = client.getOos();
                oos.writeObject(set);
                oos.flush();
                oos.reset();

            } catch (IOException  e) {
                String logClientUpdateException = thread + " " + " ";
                logger.logEvent(Level.WARNING, logClientUpdateException, LocalTime.now());
                e.printStackTrace();
            }
            System.out.println("LISTSENDER END " + thread);
        }
    }

    private String getThreadName(Thread thread){
        return thread.getName().toString();
    }
}