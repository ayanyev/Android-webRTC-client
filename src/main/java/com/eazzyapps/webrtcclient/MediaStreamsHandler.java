package com.eazzyapps.webrtcclient;

import android.graphics.Rect;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.HashMap;

import rx.Observable;
import rx.observables.ConnectableObservable;

/**
 * Created by Александр on 27.08.2016.
 */

public class MediaStreamsHandler implements MediaStreamsObserver, PeersHashMap.PeersEvents {

    public static final String VIDEO_TRACK_ID = "video";
    public static final String AUDIO_TRACK_ID = "audio";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStream";

    private GLSurfaceView glSurfaceView;
    private VideoSource localVideoSource;
    private HashMap<String, VideoRenderer.Callbacks> remoteRenders;
    private HashMap<String, PeerView> peerViews;
    private PeerConnectionFactory pcFactory;
    private MediaStream localStream;
    private ViewGroup mRoot;
    private int glWidth = 0;
    private int glHeight = 0;
    Observable<Boolean> surfaceLayoutObservable;

    public MediaStreamsHandler() {

        this.pcFactory = new PeerConnectionFactory();
        this.remoteRenders = new HashMap<>();
        this.peerViews = new HashMap<>();

    }

    public void swapGLSurfaceView(GLSurfaceView glSurfaceView, PeersHashMap peers) {

        this.glSurfaceView = glSurfaceView;

        VideoRendererGui.setView(glSurfaceView,
                () -> {

                    mRoot = (ViewGroup) glSurfaceView.getParent();
                    glWidth = glSurfaceView.getMeasuredWidth();
                    glHeight = glSurfaceView.getMeasuredHeight();

                    if (peers.size() > 0)
                        for (EAPeer peer : peers.values())
                            createImageRenderer(peer);

//                    this.glSurfaceView.addOnLayoutChangeListener(
//                            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
//                                Log.d(Constants.TAG, "layout listener called");
//                                glWidth = v.getMeasuredWidth();
//                                glHeight = v.getMeasuredHeight();
//                                resizePeerViews();
//                            });

                    surfaceLayoutObservable = Observable.create((Observable.OnSubscribe<Boolean>) subscriber ->
                            this.glSurfaceView.addOnLayoutChangeListener(
                                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                                        glWidth = v.getMeasuredWidth();
                                        glHeight = v.getMeasuredHeight();
                                        subscriber.onNext(true);
                                    }));

                    surfaceLayoutObservable
                            .skip(2)
                            .subscribe(surfaceChanged ->
                                    resizePeerViews()
                            );
                });
    }

    public void resizePeerViews() {

        if (peerViews.size() > 0) {
            Log.d(Constants.TAG, "views resized");
            for (PeerView view : peerViews.values()) {
                view.setLayoutFromPercentage(glWidth, glHeight);
            }
        }
    }

    public void createLocalMediaStream() {

        EAPeerConnectionClient pcClient = EAPeerConnectionClient.getInstance();
        MediaConstraints videoConstraints = pcClient.params.videoConstraints;
        MediaConstraints audioConstraints = pcClient.params.audioConstraints;

        // Returns the number of cams & front/back face device name
        int camNumber = VideoCapturerAndroid.getDeviceCount();
        String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String backFacingCam = VideoCapturerAndroid.getNameOfBackFacingDevice();

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturerAndroid capturer = (VideoCapturerAndroid) VideoCapturerAndroid.create(frontFacingCam);

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, videoConstraints);
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        // First we create an AudioSource then we can create our AudioTrack
        AudioSource audioSource = pcFactory.createAudioSource(audioConstraints);
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        localStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);

        Log.d(Constants.TAG, "local stream created on thread: " + Thread.currentThread().getName());
    }

    public MediaStream getLocalStream() {
        return localStream;
    }

    private PeerView createPeerView(EAPeer peer, int left, int top, int width, int height) {

        PeerView peerView = new PeerView(glSurfaceView.getContext(), peer);

        if (peer.isMyself())
            peerView.setImageLevel(PeerView.LEVEL_PROGRESS);
        else
            peerView.setImageLevel(PeerView.LEVEL_READY_TO_CONNECT);

        Rect layoutInPercentage = new Rect(left, top, Math.min(100, left + width), Math.min(100, top + height));
        peerView.setLayoutFromPercentage(layoutInPercentage, glWidth, glHeight);

        return peerView;
    }

    private void createImageRenderer(EAPeer peer) {

        VideoRenderer.Callbacks renderer;
        PeerView peerView;

        int x = 0, y = 0, w = 0, h = 0;

        if (remoteRenders.size() == 0) {
            x = 15;
            y = 15;
            w = 30;
            h = 30;
        } else if (remoteRenders.size() == 1) {
            x = 55;
            y = 15;
            w = 30;
            h = 30;
        } else if (remoteRenders.size() == 2) {
            x = 15;
            y = 55;
            w = 30;
            h = 30;
        }
        if (remoteRenders.size() == 3) {
            x = 55;
            y = 55;
            w = 30;
            h = 30;
        }

        renderer = VideoRendererGui.create(x, y, w, h,
                VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

        peerView = createPeerView(peer, x, y, w, h);
        peerView.setPeerName(peer.isMyself() ? "me" : peer.getUserName());

        remoteRenders.put(peer.getUserId(), renderer);
        peerViews.put(peer.getUserId(), peerView);

        mRoot.addView(peerView);

        //starts showing local media stream
        if (peer.isMyself()) {
            onAddStream(peer, localStream);
            Log.d(Constants.TAG, "local renderer is created and added to local media stream");
        }

//        Log.d(Constants.TAG, "renderer and view is created for peer: " + peer.getUserId() + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void onAddStream(EAPeer peer, MediaStream mediaStream) {

        VideoRenderer renderer = new VideoRenderer(remoteRenders.get(peer.getUserId()));
        PeerView peerView = peerViews.get(peer.getUserId());

        try {
            if (mediaStream.videoTracks.size() == 0) return;
            peerView.setImageLevel(PeerView.LEVEL_FRAME);
            mediaStream.videoTracks.get(0).addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(Constants.TAG, "renderer added to stream for peer: " + peer.getUserId() + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void onRemoveStream(String userId, MediaStream mediaStream) {

        //TODO consider use cases

    }

    @Override
    public void onPeerAdded(EAPeer peer) {

        createImageRenderer(peer);
        Log.d(Constants.TAG, "peer added: " + peer.getUserId() + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void onPeerRemoved(EAPeer peer) {

        String id = peer.getUserId();

        remoteRenders.remove(id);
        PeerView peerView = peerViews.get(id);
        mRoot.removeView(peerView);
        peerViews.remove(id);

        Log.d(Constants.TAG, "peer removed: " + peer.getUserId() + " on thread: " + Thread.currentThread().getName());
    }

    public void onPause() {
        try {
            glSurfaceView.onPause();
            localVideoSource.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResume() {
        try {
            glSurfaceView.onResume();
            localVideoSource.restart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
