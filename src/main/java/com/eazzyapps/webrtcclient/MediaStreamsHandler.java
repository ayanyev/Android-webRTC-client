package com.eazzyapps.webrtcclient;

import android.opengl.GLSurfaceView;
import android.util.Log;

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
import rx.Subscriber;

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
    private PeerConnectionFactory pcFactory;
    private MediaStream localStream;

    public MediaStreamsHandler(GLSurfaceView glSurfaceView, int peersNum) {

        this.glSurfaceView = glSurfaceView;
        this.pcFactory = new PeerConnectionFactory();
        this.remoteRenders = new HashMap<>();

        init();
    }

    public void init() {

        if (glSurfaceView == null)
            throw new NullPointerException();

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

    @Override
    public void onAddStream(String userId, MediaStream mediaStream) {

        try {
            if (mediaStream.videoTracks.size() == 0) return;
            mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRenders.get(userId)));
//            VideoRendererGui.update(remoteRenderSelected, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
//            VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(Constants.TAG, "renderer added to mediaStream for peer: " + userId);
    }

    @Override
    public void onRemoveStream(String userId, MediaStream mediaStream) {

        //TODO consider use cases

    }

    @Override
    public void onPeerAdded(EAPeer peer) {

        createImageRenderer(peer);
    }

    private void addRendererToLocalStream(VideoRenderer.Callbacks renderer) {

        if (localStream.videoTracks.size() != 0)
            localStream.videoTracks.get(0).addRenderer(new VideoRenderer(renderer));

        Log.d(Constants.TAG, "local renderer is created and added to local media stream");
    }

    private void createImageRenderer(EAPeer peer) {

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom

        VideoRenderer.Callbacks renderer = null;

        if (remoteRenders.size() == 0) {
            renderer = VideoRendererGui.create(15, 15, 30, 30,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        } else if (remoteRenders.size() == 1) {
            renderer = VideoRendererGui.create(55, 15, 30, 30,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        } else if (remoteRenders.size() == 2) {
            renderer = VideoRendererGui.create(15, 55, 30, 30,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }
        if (remoteRenders.size() == 3) {
            renderer = VideoRendererGui.create(55, 55, 30, 30,
                    VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }
        remoteRenders.put(peer.getUserId(), renderer);

        //starts showing local media stream
        if (peer.isMyself())
            addRendererToLocalStream(renderer);

        Log.d(Constants.TAG, "renderer is created for peer: " + peer.getUserId());
    }


    @Override
    public void onPeerRemoved(EAPeer peer) {

        remoteRenders.remove(peer.getUserId());
    }

    public void onPause() {
        glSurfaceView.onPause();
        localVideoSource.stop();
    }

    public void onResume() {
        glSurfaceView.onResume();
        localVideoSource.restart();
    }

}
