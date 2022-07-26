package entity;

import entity.Sendables;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author twgust
 */
public class ObjectBuffer {
    private final LinkedBlockingQueue<Sendables> queue;

    public ObjectBuffer(){
        queue = new LinkedBlockingQueue<>();
    }
    /**
     * Queues a client according to fifo principles
     * @param sendable object read from OIS
     */
    public synchronized void enqueueSendable(Sendables sendable) throws InterruptedException {
        queue.put(sendable);
        this.notify();
    }

    /**
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