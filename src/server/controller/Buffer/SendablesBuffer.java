package server.controller.Buffer;

import entity.Sendables;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author twgust
 *
 */
public class SendablesBuffer {
    private final LinkedBlockingQueue<Sendables> queue;

    /**
     * @author twgust
     */
    public SendablesBuffer(){
        queue = new LinkedBlockingQueue<>();
    }
    /**
     * @author twgust
     * Queues a client according to fifo principles
     * @param
     */
    public synchronized void enqueueSendable(Sendables sendable) throws InterruptedException {
        queue.put(sendable);
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
