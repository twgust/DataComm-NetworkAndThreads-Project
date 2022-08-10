package server.controller.Buffer;

import entity.Sendables;

import java.util.concurrent.LinkedBlockingQueue;

public class SendablesBuffer {
    private final LinkedBlockingQueue<Sendables> queue;

    public SendablesBuffer(){
        queue = new LinkedBlockingQueue<>();
    }
    /**
     * Queues a client according to fifo principles
     * @param
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
