package server.ServerInterface;

import java.time.LocalTime;
import java.util.logging.Level;

public interface LoggerCallBack {
    void logInfoToGui(Level level, String info, LocalTime time);

}