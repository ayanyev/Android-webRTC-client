package com.eazzyapps.webrtcclient;

import android.opengl.GLSurfaceView;
import android.util.Log;
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

/**
 * Created by Александр on 27.08.2016.
 */

public class MediaStreamsHandler implements MediaStreamsObserver, PeersHashMap.PeersEvents {

    public static final String VIDEO_TRACK_ID = "video";
    public static final String AUDIO_TRACK_ID = "audio";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStream";
    Observable<Boolean> surfaceLayoutObservable;
    private GLSurfaceView glSurfaceView;
    private VideoSource localVideoSource;
    private HashMap<String, EAPeerView> peerViews;
    private PeerConnectionFactory pcFactory;
    private MediaStream localStream;
    private ViewGroup mRoot;
    private int glWidth = 0;
    private int glHeight = 0;

    public MediaStreamsHandler() {

        this.pcFactory = new PeerConnectionFactory();
        this.peerViews = new HashMap<>();
    }

    public Observable<Boolean> setSurfaceView(GLSurfaceView glSurfaceView) {

        this.glSurfaceView = glSurfaceView;

        return Observable.create((Observable.OnSubscribe<Boolean>) rendererSubscriber -> {

            VideoRendererGui.setView(glSurfaceView,
                    () -> {

                        rendererSubscriber.onNext(true);
                        rendererSubscriber.onCompleted();

                        mRoot = (ViewGroup) glSurfaceView.getParent();
                        glWidth = glSurfaceView.getMeasuredWidth();
                        glHeight = glSurfaceView.getMeasuredHeight();

                        // observes surface view layout change on rotation
                        surfaceLayoutObservable = Observable.create((Observable.OnSubscribe<Boolean>) subscriber ->
                                this.glSurfaceView.addOnLayoutChangeListener(
                                        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                                            glWidth = v.getMeasuredWidth();
                                            glHeight = v.getMeasuredHeight();
                                            subscriber.onNext(true);
                                        }));

                        // skips first layout change called on creation
                        surfaceLayoutObservable
                                .skip(1)
                                .subscribe(surfaceChanged -> resizePeerViews());
                    });
        });
    }

    public void resizePeerViews() {

        if (peerViews.size() > 0) {
            Log.d(Constants.TAG, "views resized");
            for (EAPeerView view : peerViews.values()) {
                view.setSizeAndPosition(glWidth, glHeight);
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

    private void createPeerView(EAPeer peer) {

        int x = 0, y = 0, w = 0, h = 0;

        if (peerViews.size() == 0) {
            x = 15;
            y = 15;
            w = 30;
            h = 30;
        } else if (peerViews.size() == 1) {
            x = 55;
            y = 15;
            w = 30;
            h = 30;
        } else if (peerViews.size() == 2) {
            x = 15;
            y = 55;
            w = 30;
            h = 30;
        }
        if (peerViews.size() == 3) {
            x = 55;
            y = 55;
            w = 30;
            h = 30;
        }

        VideoRenderer renderer;

        EAPeerView peerView = new EAPeerView(glSurfaceView.getContext(), peer);
        peerView.setLayoutInPercentage(x, y, w, h);
        peerView.setSizeAndPosition(glWidth, glHeight);
        peerView.setPeerName(peer.isMyself() ? "me" : peer.getUserName());

        if (peer.isMyself())
            peerView.setImageLevel(EAPeerView.LEVEL_PROGRESS);
        else
            peerView.setImageLevel(EAPeerView.LEVEL_READY_TO_CONNECT);

        try {
            renderer = VideoRendererGui.createGui(x, y, w, h,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            peerView.setRenderer(renderer);

        } catch (Exception e) {
            e.printStackTrace();
        }

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

        EAPeerView peerView = peerViews.get(peer.getUserId());

        try {
            if (mediaStream.videoTracks.size() == 0) return;
            peerView.setImageLevel(EAPeerView.LEVEL_FRAME);
            mediaStream.videoTracks.get(0).addRenderer(peerView.getRenderer());
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

        createPeerView(peer);
        Log.d(Constants.TAG, "peer added: " + peer.getUserId() + " on thread: " + Thread.currentThread().getName());
    }

    @Override
    public void onPeerRemoved(EAPeer peer) {

        String id = peer.getUserId();

        EAPeerView peerView = peerViews.get(id);
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
