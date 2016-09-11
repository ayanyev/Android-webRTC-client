
package com.eazzyapps.webrtcclient;

/**
 * Created by Александр on 11.07.2016.
 */

public class EAPeerConnectionClient {

    SignalingServer messenger;
    EASignalingParams params;
    MediaStreamsHandler streamsHandler;

    private static EAPeerConnectionClient ourInstance = new EAPeerConnectionClient();

    public static EAPeerConnectionClient getInstance() {
        return ourInstance;
    }

    private EAPeerConnectionClient() {
    }

    public EAPeerConnection createPeerConnection(EAPeer peer) {

        if (params != null && messenger != null && streamsHandler != null) {

            EAPeerConnection pc = new EAPeerConnection(peer, params.iceServers,
                    params.pcConstraints, messenger, streamsHandler);
            pc.addStream(streamsHandler.getLocalStream());

            return pc;
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

    public void setStreamsHandler(MediaStreamsHandler streamsHandler) {
        this.streamsHandler = streamsHandler;
    }
}
