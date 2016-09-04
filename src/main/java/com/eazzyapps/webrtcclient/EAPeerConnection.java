package com.eazzyapps.webrtcclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by Александр on 25.08.2016.
 */
public class EAPeerConnection implements PeerConnection.Observer, SdpObserver {

    PeerConnectionFactory pcFactory;
    PeerConnection pc;
    SignalingServer messenger;
    String peerId;
    MediaConstraints constraints;
    MediaStreamsObserver observer;

    public EAPeerConnection(String id,
                            List<PeerConnection.IceServer> servers,
                            MediaConstraints constraints,
                            SignalingServer messenger,
                            MediaStreamsObserver observer) {
        this.peerId = id;
        this.pcFactory = new PeerConnectionFactory();
        this.pc = pcFactory.createPeerConnection(servers, constraints, this);
        this.messenger = messenger;
        this.observer = observer;
        this.constraints = constraints;
    }

    protected void addIceCandidate(IceCandidate iceCandidate) {
        pc.addIceCandidate(iceCandidate);
    }

    protected void addStream(MediaStream localStream){
        pc.addStream(localStream);
    }

    protected void createOffer() {
        pc.createOffer(this, constraints);
    }

    protected void createAnswer() {
        pc.createAnswer(this, constraints);
    }

    public void setRemoteDescription(SessionDescription remoteSdp) {

        pc.setRemoteDescription(this, remoteSdp);

        if (remoteSdp.type == SessionDescription.Type.OFFER)
            pc.createAnswer(this, constraints);
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {

    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {

        Log.d(Constants.TAG, "got ICE candidate");
        JSONObject payload = new JSONObject();
        try {
            payload.put("sdpMid", iceCandidate.sdpMid);
            payload.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
            payload.put("sdp", iceCandidate.sdp);
            messenger.sendMsg(new EAMessage(EAMessage.TYPE_CANDIDATE, peerId, payload));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

        Log.d(Constants.TAG, "got remote stream");
        observer.onAddStream(mediaStream);
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

        observer.onRemoveStream(mediaStream);
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }

    @Override
    public void onCreateSuccess(SessionDescription sdp) {

        pc.setLocalDescription(this, sdp);

        String msgType;

        if (sdp.type == SessionDescription.Type.OFFER) {
            Log.d(Constants.TAG, "Offer created");
            msgType = EAMessage.TYPE_OFFER;
        } else {
            Log.d(Constants.TAG, "Answer created");
            msgType = EAMessage.TYPE_ANSWER;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("type", sdp.type.canonicalForm());
            payload.put("sdp", sdp.description);
            messenger.sendMsg(new EAMessage(msgType, peerId, payload));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSetSuccess() {

    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(Constants.TAG, s);
    }

    @Override
    public void onSetFailure(String s) {

    }

}
