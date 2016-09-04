package com.eazzyapps.webrtcclient;

import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rx.Observable;

/**
 * Created by Александр on 26.07.2016.
 */
public class EASignalingParams {

    public List<PeerConnection.IceServer> iceServers;
    public final MediaConstraints pcConstraints;
    public final MediaConstraints videoConstraints;
    public final MediaConstraints audioConstraints;



    public EASignalingParams(
            List<PeerConnection.IceServer> iceServers,
            MediaConstraints pcConstraints,
            MediaConstraints videoConstraints,
            MediaConstraints audioConstraints) {
        this.iceServers = (iceServers == null) ? defaultIceServers() : iceServers;
        this.pcConstraints = (pcConstraints == null) ? defaultPcConstraints() : pcConstraints;
        this.videoConstraints = (videoConstraints == null) ? defaultVideoConstraints() : videoConstraints;
        this.audioConstraints = (audioConstraints == null) ? defaultAudioConstraints() : audioConstraints;
    }

    /**
     * Default Ice Servers, but specified parameters.
     *
     * @param pcConstraints
     * @param videoConstraints
     * @param audioConstraints
     */
    public EASignalingParams(
            MediaConstraints pcConstraints,
            MediaConstraints videoConstraints,
            MediaConstraints audioConstraints) {
        this.iceServers = EASignalingParams.defaultIceServers();
        this.pcConstraints = (pcConstraints == null) ? defaultPcConstraints() : pcConstraints;
        this.videoConstraints = (videoConstraints == null) ? defaultVideoConstraints() : videoConstraints;
        this.audioConstraints = (audioConstraints == null) ? defaultAudioConstraints() : audioConstraints;
    }

    /**
     * Default media params, but specified Ice Servers
     *
     * @param iceServers
     */
    public EASignalingParams(List<PeerConnection.IceServer> iceServers) {
        this.iceServers = defaultIceServers();
        this.pcConstraints = defaultPcConstraints();
        this.videoConstraints = defaultVideoConstraints();
        this.audioConstraints = defaultAudioConstraints();
        addIceServers(iceServers);
    }

    /**
     * Default media params and ICE servers.
     */
    public EASignalingParams() {
        this.iceServers = defaultIceServers();
        this.pcConstraints = defaultPcConstraints();
        this.videoConstraints = defaultVideoConstraints();
        this.audioConstraints = defaultAudioConstraints();
    }

    /**
     * The default parameters for media constraints. Might have to tweak in future.
     *
     * @return default parameters
     */
    public static EASignalingParams defaultInstance() {
        MediaConstraints pcConstraints = EASignalingParams.defaultPcConstraints();
        MediaConstraints videoConstraints = EASignalingParams.defaultVideoConstraints();
        MediaConstraints audioConstraints = EASignalingParams.defaultAudioConstraints();
        List<PeerConnection.IceServer> iceServers = EASignalingParams.defaultIceServers();
        return new EASignalingParams(iceServers, pcConstraints, videoConstraints, audioConstraints);
    }

    private static MediaConstraints defaultPcConstraints() {
        MediaConstraints pcConstraints = new MediaConstraints();
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        return pcConstraints;
    }

    private static MediaConstraints defaultVideoConstraints() {
        MediaConstraints videoConstraints = new MediaConstraints();
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", "1280"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", "720"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", "640"));
        videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", "480"));
        return videoConstraints;
    }

    private static MediaConstraints defaultAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        return audioConstraints;
    }

    public static Observable<List<PeerConnection.IceServer>> getXirSysIceServers() {
        List<PeerConnection.IceServer> servers;
        try {
            servers = new EAXirSysRequest().execute().get();
        } catch (InterruptedException | ExecutionException e) {
            servers = defaultIceServers();
        }
        return Observable.just(servers);
    }

    public static List<PeerConnection.IceServer> defaultIceServers() {

        //        List<PeerConnection.IceServer> servers = new ArrayList<>();
//        servers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
//        servers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
//        servers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
//        servers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
//        servers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
//        servers.add(new PeerConnection.IceServer("turn:numb.viagenie.ca", "eazzyapps@gmail.com", "htabbas"));
//        servers.add(new PeerConnection.IceServer("turn:192.158.29.39:3478?transport=tcp", "28224511:1379330808", "JZEOEt2V3Qb0y27GRntt2u2PAYA="));
//        servers.add(new PeerConnection.IceServer("turn:192.158.29.39:3478?transport=udp", "28224511:1379330808", "JZEOEt2V3Qb0y27GRntt2u2PAYA="));


        //TODO default list
        List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>(25);
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.services.mozilla.com"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.bistri.com:80", "homeo", "homeo"));
        iceServers.add(new PeerConnection.IceServer("turn:turn.anyfirewall.com:443?transport=tcp", "webrtc", "webrtc"));

        // Extra Defaults - 19 STUN servers + 4 initial = 23 severs (+2 padding) = Array cap 25
        iceServers.add(new PeerConnection.IceServer("stun:stun1.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun2.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun3.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:stun4.l.google.com:19302"));
        iceServers.add(new PeerConnection.IceServer("stun:23.21.150.121"));
        iceServers.add(new PeerConnection.IceServer("stun:stun01.sipphone.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ekiga.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.fwdnet.net"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.ideasip.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.iptel.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.rixtelecom.se"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.schlund.de"));
        iceServers.add(new PeerConnection.IceServer("stun:stunserver.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.softjoys.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voiparound.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipbuster.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voipstunt.com"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.voxgratia.org"));
        iceServers.add(new PeerConnection.IceServer("stun:stun.xten.com"));

        return iceServers;
    }

    public void swapIceServers(List<PeerConnection.IceServer> iceServers) {

        this.iceServers = new ArrayList<>();
        this.iceServers.addAll(iceServers);
    }

    /**
     * Append default servers to the end of given list and set as iceServers instance variable
     *
     * @param iceServers List of iceServers
     */
    public void addIceServers(List<PeerConnection.IceServer> iceServers) {
        if (this.iceServers != null) {
            iceServers.addAll(this.iceServers);
        }
        this.iceServers = iceServers;
    }

    /**
     * Instantiate iceServers if they are not already, and add Ice Server to beginning of list.
     *
     * @param iceServers Ice Server to add
     */
    public void addIceServers(PeerConnection.IceServer iceServers) {
        if (this.iceServers == null) {
            this.iceServers = new ArrayList<>();
        }
        this.iceServers.add(0, iceServers);
    }
}