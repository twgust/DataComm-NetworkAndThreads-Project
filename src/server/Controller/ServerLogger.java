package server.Controller;


import server.ServerInterface.LoggerCallBack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Date;
import java.util.logging.Level;

public class ServerLogger {
    private Date date;
    private FileWriter fw;
    private StringBuilder builder;
    private LoggerCallBack loggerCallback;
    public ServerLogger() throws IOException {
        File directory = new File("log");
        if (! directory.exists()){
            directory.mkdir();
        }
        date = new Date();
        fw = new FileWriter("log/"+date.getTime()+".txt");
    }
    public synchronized void logEvent(Level level,String thread, String info, LocalTime time) {
        String builtString = buildString(level, thread, info, time);
        loggerCallback.logInfoToGui(builtString, level);
        try{
            logEventToFile(builtString);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public void logEventToFile(String info) throws IOException {
        fw.write(info);
        fw.flush();
    }
    private synchronized String buildString(Level level,String thread, String info, LocalTime time){
        builder = new StringBuilder();
        return builder.append("SERVER: [").append(time.getHour()).append(':')
                .append(time.getMinute()).append(':')
                .append(time.getSecond()).append(']').append(' ')
                .append(' ').append(level.getName()).append(' ').append(" ")
                .append(thread).append(" - - - << ").append(info).append("\n\n").toString();
    }

    public void setLoggerCallback(LoggerCallBack impl) {
        loggerCallback = impl;
    }

}
