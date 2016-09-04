package com.eazzyapps.webrtcclient;

import org.webrtc.MediaStream;

import java.util.HashMap;

/**
 * Created by Александр on 15.07.2016.
 */
public interface EAClientListener {

    void onConnected();
    void onConnectionError(String msg);
    void onPeersDiscovered(HashMap<String, EAPeer> peers);
    void onNewPeer();
    void onError(String e);

}
