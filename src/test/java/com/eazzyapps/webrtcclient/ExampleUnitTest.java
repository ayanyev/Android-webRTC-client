package com.eazzyapps.webrtcclient;

import org.junit.Before;
import org.junit.Test;
import org.webrtc.PeerConnectionFactory;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    EAWebRTCClient client;
    HashMap<String, EAPeer> peers;

    @Before
    public void runBefore(){

/*        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context*/

//        EAWebRTCClient client = new EAWebRTCClient("testUser");
//        client.init(null);
    }

    @Test(timeout = 3000)
    public void get_peers_from_server() throws Exception {

        peers = client.getPeers();
        assertTrue(peers.size()>0);
    }
}