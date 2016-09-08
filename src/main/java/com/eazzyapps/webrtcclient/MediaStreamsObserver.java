package com.eazzyapps.webrtcclient;

import org.webrtc.MediaStream;

/**
 * Created by Александр on 27.08.2016.
 */
public interface MediaStreamsObserver {

    void onAddStream(String userId, MediaStream mediaStream);
    void onRemoveStream(String userId, MediaStream mediaStream);


}
