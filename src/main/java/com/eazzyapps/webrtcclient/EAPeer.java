package com.eazzyapps.webrtcclient;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by Александр on 11.07.2016.
 */
public class EAPeer {

    private String userId;
    private String userName;
    private EAPeerConnection pc;
    private boolean connected;
    private boolean myself;

    public EAPeer(String userId, String userName)  {

        this.userId = userId;
        this.userName = userName;
        this.myself = false;
        this.connected = false;
        this.pc = EAPeerConnectionClient.getInstance().createPeerConnection(userId);
    }

    public EAPeerConnection getPeerConnection() {
        return pc;
    }

    public void setPeerConnection(EAPeerConnection pc) {
        this.pc = pc;
    }

    public void addIceCandidate(IceCandidate iceCandidate){
        getPeerConnection().addIceCandidate(iceCandidate);
    }

    public void createOffer(){
        getPeerConnection().createOffer();
    }

    public void setRemoteDescription(SessionDescription remoteSdp) {
        getPeerConnection().setRemoteDescription(remoteSdp);
    }

    public void createAnswer(){
        getPeerConnection().createAnswer();
    }

    public String getUserName() {
        return userName;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isMyself() {
        return myself;
    }

    public void setMyself(boolean myself) {
        this.myself = myself;
    }
}
