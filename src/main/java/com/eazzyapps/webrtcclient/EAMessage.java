package com.eazzyapps.webrtcclient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Александр on 25.08.2016.
 */
public class EAMessage {

    public static final String TYPE_OPEN = "OPEN";
    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_OFFER = "OFFER";
    public static final String TYPE_ANSWER = "ANSWER";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_ID_TAKEN = "ID-TAKEN";
    public static final String TYPE_CANDIDATE = "CANDIDATE";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
    public static final String TYPE_NEW_USER = "NEW_USER";

    String type;
    String source;
    String destination;
    String payload;

    public EAMessage(String type, String destination, JSONObject payload) {
        this.type = type;
        this.destination = destination;
        if (payload != null)
            this.payload = payload.toString();
    }

    public EAMessage(Throwable e){
        this.type = TYPE_ERROR;
        this.payload = e.getMessage();
    }

    public EAMessage(String message) throws JSONException {

        JSONObject o = new JSONObject(message);
        type = o.getString("type");
        if (!type.equals(TYPE_OPEN)) {
            source = o.getString("src");
            destination = o.getString("dst");
            payload = o.getString("payload");
        }
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {

        JSONObject o = new JSONObject();
        try {
            o.put("type", type);
            o.put("src", source != null ? source : "");
            o.put("dst", destination != null ? destination : "");
            o.put("payload", payload != null ? payload : "");
            return o.toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
