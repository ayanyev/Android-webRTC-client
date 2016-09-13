package com.eazzyapps.webrtcclient;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by Александр on 27.08.2016.
 */

public class MediaStreamsHandler implements MediaStreamsObserver, PeersHashMap.PeersEvents, GLSurfaceView.Renderer {

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
    private int glX;
    private int glY;
    private int glWidth;
    private int glHeight;

    public MediaStreamsHandler(GLSurfaceView glSurfaceView, int peersNum) {

        this.glSurfaceView = glSurfaceView;
        this.pcFactory = new PeerConnectionFactory();
        this.remoteRenders = new HashMap<>();
        this.peerViews = new HashMap<>();

        init();
    }

    public void init() {

        if (glSurfaceView == null)
            throw new NullPointerException();

        mRoot = (ViewGroup) glSurfaceView.getParent();

        // Then we set that view, and pass a Runnable to run once the surface is ready
        VideoRendererGui.setView(glSurfaceView, null);
        createLocalMediaStream();
    }

    private void createLocalMediaStream() {

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
        Log.d(Constants.TAG, "local media stream created");

        // Now we can add our tracks.
        localStream.addTrack(localVideoTrack);
        localStream.addTrack(localAudioTrack);
    }

    public MediaStream getLocalStream() {
        return localStream;
    }

    private PeerView createPeerView(EAPeer peer, int left, int top, int width, int height) {

        glWidth = glSurfaceView.getMeasuredWidth();
        glHeight = glSurfaceView.getMeasuredHeight();

        // derived from VideoRendererGui
        Rect displayLayout = new Rect();
        Rect layoutInPercentage = new Rect(left, top, Math.min(100, left + width), Math.min(100, top + height));

        displayLayout.set(
                (glWidth * layoutInPercentage.left + 99) / 100,
                (glHeight * layoutInPercentage.top + 99) / 100,
                glWidth * layoutInPercentage.right / 100,
                glHeight * layoutInPercentage.bottom / 100);

        displayLayout.inset(-10, -10);

//        float videoAspectRatio = this.rotationDegree % 180 == 0?(float)this.videoWidth / (float)this.videoHeight:(float)this.videoHeight / (float)this.videoWidth;
//        float minVisibleFraction = convertScalingTypeToVisibleFraction(this.scalingType);
//        Point displaySize = getDisplaySize(minVisibleFraction, videoAspectRatio, this.displayLayout.width(), this.displayLayout.height());
//        displayLayout.inset((this.displayLayout.width() - displaySize.x) / 2, (this.displayLayout.height() - displaySize.y) / 2);


        PeerView peerView = new PeerView(glSurfaceView.getContext(), peer);

        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        displayLayout.right - displayLayout.left,
                        displayLayout.bottom - displayLayout.top);

        params.leftMargin = displayLayout.left;
        params.topMargin = displayLayout.top;

        peerView.setLayoutParams(params);

        return peerView;
    }

    @Override
    public void onAddStream(EAPeer peer, MediaStream mediaStream) {

        VideoRenderer renderer = new VideoRenderer(remoteRenders.get(peer.getUserId()));
        PeerView peerView = peerViews.get(peer.getUserId());

        try {
            if (mediaStream.videoTracks.size() == 0) return;
            peerView.setImageLevel(0);
            mediaStream.videoTracks.get(0).addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(Constants.TAG, "renderer added to mediaStream for peer: " + peer.getUserId());
    }

    @Override
    public void onRemoveStream(String userId, MediaStream mediaStream) {

        //TODO consider use cases

    }

    @Override
    public void onPeerAdded(EAPeer peer) {

        createImageRenderer(peer);
    }

    @Override
    public void onPeerRemoved(EAPeer peer) {

        String id = peer.getUserId();

        remoteRenders.remove(id);
        PeerView peerView = peerViews.get(id);
        mRoot.removeView(peerView);
        peerViews.remove(id);
    }

    private void addRendererToLocalStream(VideoRenderer.Callbacks renderer) {

        if (localStream.videoTracks.size() != 0)
            localStream.videoTracks.get(0).addRenderer(new VideoRenderer(renderer));

        Log.d(Constants.TAG, "local renderer is created and added to local media stream");
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
        if (peer.isMyself())
            addRendererToLocalStream(renderer);

        Log.d(Constants.TAG, "renderer is created for peer: " + peer.getUserId());
    }

    public void onPause() {
        glSurfaceView.onPause();
        localVideoSource.stop();
    }

    public void onResume() {
        glSurfaceView.onResume();
        localVideoSource.restart();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        glWidth = width;
        glHeight = height;

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
