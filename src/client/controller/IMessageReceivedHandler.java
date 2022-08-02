package client.controller;

import entity.Message;

import java.time.LocalTime;

public interface IMessageReceivedHandler {
    public void textMessageReceived(Message message, LocalTime timeNow);
    public void imageMessageReceived(Message message, LocalTime timeNow);
    public void txtAndImgMessageReceived(Message message, LocalTime timeNow);
}
