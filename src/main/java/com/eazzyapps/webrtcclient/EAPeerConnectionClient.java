
package com.eazzyapps.webrtcclient;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import java.util.List;

/**
 * Created by Александр on 11.07.2016.
 */

public class EAPeerConnectionClient {

    SignalingServer messenger;
    EASignalingParams params;
    MediaStreamsObserver streamsObserver;

    private static EAPeerConnectionClient ourInstance = new EAPeerConnectionClient();

    public static EAPeerConnectionClient getInstance() {
        return ourInstance;
    }

    private EAPeerConnectionClient() {
    }

    public EAPeerConnection createPeerConnection(String peerId) {

        if (params != null && messenger != null && streamsObserver != null) {

            return new EAPeerConnection(peerId, params.iceServers, params.pcConstraints, messenger, streamsObserver);
        }
        return null;
    }

    public SignalingServer getMessenger() {
        return messenger;
    }

    public void setMessenger(SignalingServer messenger) {
        this.messenger = messenger;
    }

    public void setParams(EASignalingParams params) {
        this.params = params;
    }

    public void setStreamsObserver(MediaStreamsObserver streamsObserver) {
        this.streamsObserver = streamsObserver;
    }
}
