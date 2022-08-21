package server.Controller.Buffer;

import entity.Sendables;

import java.util.ArrayDeque;

/**
 * @author twgust
 *
 */
public class SendableBuffer {
    private final ArrayDeque<Sendables> queue;

    /**
     * @author twgust
     */
    public SendableBuffer(){
        queue = new ArrayDeque<>();
    }
    /**
     * @author twgust
     * Queues a client according to fifo principles
     * @param sendable to queue for processing.
     */
    public synchronized void enqueueSendable(Sendables sendable) throws InterruptedException {
        queue.addLast(sendable);
        this.notify();
    }

    /**
     * cheat queue and put it to first, used for user connections and updated UserSet(s)
     * @param sendables UserSet
     * @throws InterruptedException
     */
    public synchronized void putFirst(Sendables sendables) throws InterruptedException {
        queue.addFirst(sendables);
        this.notify();
    }

    /**
     * @author twgust
     * @return process client according to FIFO principle
     * @throws InterruptedException wait can throw interrupted-exception of thread is interrupted
     */
    public synchronized Sendables dequeueSendable() throws InterruptedException  {
        while(queue.isEmpty()) {
            System.out.println("waiting");
            this.wait();
        }
        return queue.poll();
    }
}
