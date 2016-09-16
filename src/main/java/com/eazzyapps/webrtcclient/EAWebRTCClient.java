package com.eazzyapps.webrtcclient;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.tbruyelle.rxpermissions.RxPermissions;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
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
    private PeersHashMap peers;
    private EAPeer me;
    private String userName;
    private String userId;
    private EASignalingParams signalingParams;
    private SignalingServer server;
    private boolean connected;

    private MediaStreamsHandler streamsHandler;
    private EAPeerConnectionClient pcClient;

    public EAWebRTCClient(EAClientListener listener, String userName) {

        this.listener = listener;
        this.connected = false;
        this.userName = userName;
        try {
            this.server = new SignalingServer(Constants.herokuServerURL);
        } catch (MalformedURLException e) {
            this.server = null;
        }
        this.pcClient = EAPeerConnectionClient.getInstance();
        this.streamsHandler = new MediaStreamsHandler();
        this.signalingParams = new EASignalingParams();
        // params with default values to be available for MediaStreamHandler
        this.pcClient.setParams(signalingParams);
        this.peers = new PeersHashMap();
        // sets default maximum peers number
        this.maxPeersNum = 4;

        // permissions check
        RxPermissions.getInstance(listener.getListenerContext())
                .request("android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO")
                .subscribe(granted -> {
                    if (granted) {

                        init();

                    } else {
                        listener.onError("permissions not granted");
                    }
                });
    }

    public void init() {

        streamsHandler.createLocalMediaStream();

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

    private Subscription wsSubscription;

    private Subscriber<EAMessage> wsSubscriber = new Subscriber<EAMessage>() {


        @Override
        public void onNext(EAMessage message) {

            JSONObject payload;
            switch (message.type) {

                case TYPE_OPEN:

                    connected = true;

                    // new peer in map - device user himself
                    me = new EAPeer(userId, userName);
                    me.setMyself(true);
                    peers.put(userId, me);

                    discoverPeers();

                    if (listener != null)
                        listener.onConnected();
                    Log.d(Constants.TAG, "webSocket on" + " thread: " + Thread.currentThread().getName());

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

                        Log.d(Constants.TAG, "candidate received" + " thread: " + Thread.currentThread().getName());

                    } catch (JSONException e) {
                        if (listener != null) {
                            listener.onError("candidate parse error");
                            Log.d(Constants.TAG, "CANDIDATE: " + e.getMessage());
                        }
                    }
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

                        Log.d(Constants.TAG, "got OFFER from " + message.source + " thread: " + Thread.currentThread().getName());

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

                        Log.d(Constants.TAG, "got ANSWER from " + message.source + " thread: " + Thread.currentThread().getName());

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


                case TYPE_LEAVE:

                    listener.onPeerLeave(peers.get(message.source));
                    peers.remove(message.source);

                    Log.d(Constants.TAG, "peer removed: " + message.source + " thread: " + Thread.currentThread().getName());

                    return;

                case TYPE_NEW_USER:

                    peers.put(message.source,
                            new EAPeer(message.source, message.payload));

                    listener.onNewPeer();

                    Log.d(Constants.TAG, "peer added: " + message.source + " thread: " + Thread.currentThread().getName());
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

    public void setGLSurfaceView(GLSurfaceView glSurfaceView) {

        //sets glSurfaceView to be used by VideoRendererGUI
        streamsHandler.swapGLSurfaceView(glSurfaceView, peers);

        // MediaStreamHandler are now able to observe
        pcClient.setStreamsHandler(streamsHandler);
        peers.setObserver(streamsHandler);
    }

    public void setMaxPeersNum(int maxPeersNum) {
        this.maxPeersNum = maxPeersNum;
    }

    public void discoverPeers() {

        server.discoverPeers()
                .subscribeOn(Schedulers.io())
                .map(peersOnline -> {
                    for (String p : peersOnline) {

                        String name = p.split("_")[0];
                        String id = p.split("_")[1];

                        if (!id.equals(me.getUserId()))
                            peers.put(id, new EAPeer(id, name));
                    }
                    return peers;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        peers -> {
                            if (listener != null) listener.onPeersDiscovered(peers);
                        },
                        throwable -> listener.onError(throwable.getMessage())
                );
    }

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

        // MediaStreamHandler not able to observe until activity is recreated
        pcClient.setStreamsHandler(null);
        peers.setObserver(null);
    }

    public HashMap<String, EAPeer> getPeers() {
        return peers;
    }

    public boolean isConnected() {
        return connected;
    }


}
