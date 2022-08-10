package client;

import client.controller.ClientController;
import client.view.ClientGUI;
import entity.MessageType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunClient {
    public static void main(String[] args)  {

        System.out.println("MAIN - Starting clients");
        // distinguish between local clients by port number, not remote address// gui starts connection to server because a user will click connect

        String ip = "127.0.0.1";
        ClientController c1 = new ClientController(ip, 33062, "CLIENT-1");
        ClientController c2 = new ClientController(ip, 33062, "CLIENT-2");
        ClientController c3 = new ClientController(ip, 33062, "CLIENT-3");
        ClientController c4 = new ClientController(ip, 33062, "CLIENT-4");
        ClientController c5 = new ClientController(ip, 33062, "CLIENT-5");
        ClientGUI g1 = new ClientGUI(c1);
        ClientGUI g2 = new ClientGUI(c2);
        ClientGUI g3 = new ClientGUI(c3);
        ClientGUI g4 = new ClientGUI(c4);
        ClientGUI g5 = new ClientGUI(c5);
        ExecutorService threadpool = Executors.newFixedThreadPool(25);
        threadpool.submit(()->{
            g1.connect("user-6", "src/client/images/circle_of_fifths.jpg");
            //g1.sendMessage("damn that's not great usage of threads", MessageType.TEXT);

        });
        threadpool.submit(()->{
            g2.connect("user-7", "src/client/images/circle_of_fifths.jpg");

        });
        threadpool.submit(()->{
            g3.connect("user-8","src/client/images/circle_of_fifths.jpg");
            //  g3.sendMessage("hellloooo im u8", MessageType.TEXT);

        });
        threadpool.submit(()->{
            g4.connect("user-9", "src/client/images/music-circle-of-fifths.jpg");

        });
        threadpool.submit(()->{
            g5.connect("user-10","src/client/images/circle_of_fifths.jpg");

        });
        threadpool.submit(()->{
            //  g2.sendMessage("agree", MessageType.TEXT);

        });
        try{
            Thread.sleep(5000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        threadpool.submit(()->{
            g2.sendMessage("heloooo im u7","", MessageType.TEXT);
            //    g3.sendMessage("hellloooo im u8", MessageType.TEXT);
        });
        threadpool.submit(()->{
            g3.sendMessage("AAA", "",MessageType.TEXT);
            g3.sendMessage("AAA", "",MessageType.TEXT);
            g3.sendMessage("AAA", "",MessageType.TEXT);
            g3.sendMessage("AAA", "",MessageType.TEXT);
            g3.sendMessage("AAA", "",MessageType.TEXT);


        });
        threadpool.submit(()->{
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
            g4.sendMessage("BBB", "",MessageType.TEXT);
        });
        threadpool.submit(()->{
            g5.sendMessage("CCC", null,MessageType.TEXT);
            g5.sendMessage("CCC", null,MessageType.TEXT);
            g5.sendMessage("CCC", null,MessageType.TEXT);
            g5.sendMessage("CCC", null,MessageType.TEXT);
            g5.sendMessage("CCC", null,MessageType.TEXT);
            g5.sendMessage("CCC", null,MessageType.TEXT);
        });
        threadpool.submit(()->{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            g1.sendMessage("image message!!","src/client/images/cat.jpg",MessageType.TEXT_IMAGE );
        });
    }


}
