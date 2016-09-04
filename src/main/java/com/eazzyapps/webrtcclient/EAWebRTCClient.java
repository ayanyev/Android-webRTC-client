package com.eazzyapps.webrtcclient;

import android.opengl.GLSurfaceView;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

import java.net.MalformedURLException;
import java.util.HashMap;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.eazzyapps.webrtcclient.EAMessage.*;

/**
 * Created by Александр on 04.07.2016.
 */

public class EAWebRTCClient {

    private int maxPeersNum;
    private EAClientListener listener;
    HashMap<String, EAPeer> peers;
    EAPeer me;
    String userName;
    String userId;
    EASignalingParams signalingParams;
    SignalingServer server;
    boolean connected;

    private PeerConnectionFactory pcFactory;
    private MediaStreamsHandler streamsHandler;
    private EAPeerConnectionClient pcClient;

    public EAWebRTCClient(String userName) {

        this.connected = false;
        this.userName = userName;
        try {
            this.server = new SignalingServer(Constants.localServerURL_2);
        } catch (MalformedURLException e) {
            this.server = null;
        }
        this.peers = new HashMap<>();
        this.pcFactory = new PeerConnectionFactory();
        this.pcClient = EAPeerConnectionClient.getInstance();
        // sets default maximum peers number
        this.maxPeersNum = 4;
    }

    public EAWebRTCClient(EAClientListener listener, String userName) {
        this(userName);
        this.listener = listener;
    }

    Subscription wsSubscription;

    Subscriber<EAMessage> wsSubscriber = new Subscriber<EAMessage>() {

        @Override
        public void onNext(EAMessage message) {

            JSONObject payload;
            switch (message.type) {

                case TYPE_OPEN:

                    connected = true;
                    if (listener != null)
                        listener.onConnected();
                    Log.d(Constants.TAG, "webSocket on");

                    // new peer in map - device user himself
                    me = new EAPeer(userId, userName);
                    me.setMyself(true);
                    peers.put(userId, me);

                    discoverPeers();

                    return;

                case TYPE_CANDIDATE:

                    try {
                        payload = new JSONObject(message.payload);
                        IceCandidate ic = new IceCandidate(
                                payload.getString("sdpMid"),
                                payload.getInt("sdpMLineIndex"),
                                payload.getString("sdp"));

                        EAPeer peer = peers.get(message.source);
                        peer.addIceCandidate(ic);

                        Log.d(Constants.TAG, "candidate received");

                    } catch (JSONException e) {
                        if (listener != null) {
                            listener.onError("candidate parse error");
                            Log.d(Constants.TAG, "CANDIDATE: " + e.getMessage());
                        }
                    }
                    return;

                case TYPE_LEAVE:

                    return;

                case TYPE_OFFER:

                    try {
                        payload = new JSONObject(message.payload);
                        SessionDescription remoteSdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                payload.getString("sdp"));

                        EAPeer peer = peers.get(message.source);
                        peer.setRemoteDescription(remoteSdp);
                        peer.createAnswer();

                        Log.d(Constants.TAG, "got OFFER from " + message.source);

                    } catch (JSONException e) {
                        if (listener != null) {
                            listener.onError("offer parse error");
                            Log.d(Constants.TAG, "OFFER: " + e.getMessage());
                        }
                    }

                    return;

                case TYPE_ANSWER:

                    try {
                        payload = new JSONObject(message.payload);
                        SessionDescription remoteSdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                                payload.getString("sdp"));

                        EAPeer peer = peers.get(message.source);
                        peer.setRemoteDescription(remoteSdp);

                        Log.d(Constants.TAG, "got ANSWER from " + message.source);

                    } catch (JSONException e) {
                        if (listener != null) {
                            listener.onError("answer parse error");
                            Log.d(Constants.TAG, "ANSWER: " + e.getMessage());
                        }
                    }

                    return;

                case TYPE_ERROR:

                    if (listener != null) {
                        listener.onError(message.payload);
                        Log.d(Constants.TAG, "ERROR: " + message.payload);
                    }
                    return;

                case TYPE_NEW_USER:

                    EAPeerConnection pc = pcClient.createPeerConnection(message.source);
                    pc.addStream(streamsHandler.getLocalMediaStream());

                    EAPeer peer = new EAPeer(message.source, message.payload);
                    peer.setPeerConnection(pc);

                    peers.put(message.source, peer);
                    Log.d(Constants.TAG, "new peer with id: " + message.source);
            }
        }

        @Override
        public void onCompleted() {

            Log.d(Constants.TAG, "onCompleted");
        }

        @Override
        public void onError(Throwable e) {

            listener.onError(e.getMessage());
        }
    };


    public void onPause() {
        if (streamsHandler != null)
            streamsHandler.onPause();
    }

    public void onResume() {
        if (streamsHandler != null)
            streamsHandler.onResume();
    }

    public void onDestroy() {
        if (wsSubscription != null)
            wsSubscription.unsubscribe();
    }

    public void init(GLSurfaceView glSurfaceView) {

        // params with default values to be available for MediaStreamHandler
        signalingParams = new EASignalingParams();
        pcClient.setParams(signalingParams);

        if (glSurfaceView != null) {
            this.streamsHandler = new MediaStreamsHandler(glSurfaceView, maxPeersNum);
            pcClient.setStreamsObserver(streamsHandler);
        }

        // if signaling server not valid stop execution here
        if (server == null) {
            if (listener != null)
                listener.onError("server url is not valid");
            return;
        }

        wsSubscription = Observable
                .zip(
                        server.connectToServer(Constants.apiKey, userName),
                        EASignalingParams.getXirSysIceServers(),
                        (userId, servers) -> {

                            this.userId = userId;

                            // servers instead of default
                            signalingParams.swapIceServers(servers);
                            pcClient.setMessenger(server);

                            return true;
                        })
                .flatMap(isReady -> server.establishWebSocketConnection(userId, userName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wsSubscriber);
    }

    public void setMaxPeersNum(int maxPeersNum) {
        this.maxPeersNum = maxPeersNum;
    }

    public void discoverPeers() {

        server.discoverPeers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(peersOnline -> {

                            EAPeerConnection pc;
                            EAPeer peer;

                            for (String p : peersOnline) {
                                String name = p.split("_")[0];
                                String id = p.split("_")[1];

                                pc = pcClient.createPeerConnection(id);
                                pc.addStream(streamsHandler.getLocalMediaStream());

                                peer = new EAPeer(id, name);
                                peer.setPeerConnection(pc);

                                peers.put(id, peer);
                            }

                            peers.remove(me.getUserId());
                            if (listener != null)
                                listener.onPeersDiscovered(peers);
                        },
                        throwable -> listener.onError(throwable.getMessage()));
    }

    public HashMap<String, EAPeer> getPeers() {
        return peers;
    }

    public boolean isConnected() {
        return connected;
    }


}
