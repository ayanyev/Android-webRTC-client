package com.eazzyapps.webrtcclient;

/**
 * Created by Александр on 20.07.2016.
 */
public class ClientResponse {

    public ClientResponse(Message message, Object data) {
        this.message = message;
        this.data = data;
    }

    public ClientResponse(Message message) {
        this.message = message;
    }

    public enum Message {
        GOT_IP ("got id"),
        PEERS_DISCOVERED ("peers discovered"),
        NEW_PEER ("new peer"),
        CONNECTED ("connected"),
        DISCONNECTED ("disconnected");

        private String value;
        public String value(){return value;}

        Message(String value){
            this.value = value;
        }
    }

    private Message message;
    private Object data;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
