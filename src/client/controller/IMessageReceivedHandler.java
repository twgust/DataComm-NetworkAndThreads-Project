package client.controller;

import entity.Message;

public interface IMessageReceivedHandler {
    public void textMessageReceived(Message message);
    public void imageMessageReceived(Message message);
    public void txtAndImgMessageReceived(Message message);
}
