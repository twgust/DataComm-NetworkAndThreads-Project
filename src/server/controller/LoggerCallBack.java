package server.controller;

import java.util.logging.Level;

public interface LoggerCallBack {
    void logInfoToGui(Level level, String color, String info);
}
