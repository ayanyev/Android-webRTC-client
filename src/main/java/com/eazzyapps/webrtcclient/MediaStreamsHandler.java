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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Александр on 27.08.2016.
 */
public class MediaStreamsHandler implements MediaStreamsObserver {

    public static final String VIDEO_TRACK_ID = "video";
    public static final String AUDIO_TRACK_ID = "audio";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStream";

    private GLSurfaceView glSurfaceView;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender, remoteRenderSelected;
    private List<VideoRenderer.Callbacks> remoteRendersSmall;
    private PeerConnectionFactory pcFactory;
    private MediaStream localMediaStream;
    private int maxPeersNum;

    public MediaStreamsHandler(GLSurfaceView glSurfaceView, int peersNum) {

        this.glSurfaceView = glSurfaceView;
        this.maxPeersNum = peersNum;
        this.pcFactory = new PeerConnectionFactory();
        init();
    }

    public void init() {

        createLocalMediaStream();
        createImageRenderers();
    }

    private void createImageRenderers() {

        if (glSurfaceView != null) {
            // Then we set that view, and pass a Runnable to run once the surface is ready
            VideoRendererGui.setView(glSurfaceView, null);

            // Now that VideoRendererGui is ready, we can get our VideoRenderer.
            // IN THIS ORDER. Effects which is on top or bottom

            remoteRendersSmall = new ArrayList<>();
            int pos = 0;
            int s = 100 / maxPeersNum;

            localRender = VideoRendererGui.create(20, 20, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

            if (localMediaStream.videoTracks.size() != 0)
                localMediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));

            remoteRenderSelected = VideoRendererGui.create(60, 20, 30, 30, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

            for (int i = 0; i < maxPeersNum - 1; i++) {
                remoteRendersSmall.add(i, VideoRendererGui.create(60, pos + 3, s - 6, s - 6,
                        VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false));
                pos = pos + s;
            }
        }

        if (localMediaStream.videoTracks.size() == 0) return;
        localMediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
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

        localMediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        Log.d(Constants.TAG, "local media stream created");

        // Now we can add our tracks.
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);
    }

    public MediaStream getLocalMediaStream() {
        return localMediaStream;
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {

        try {
            if (mediaStream.videoTracks.size() == 0) return;
            mediaStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRenderSelected));
//            VideoRendererGui.update(remoteRenderSelected, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
//            VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {

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
