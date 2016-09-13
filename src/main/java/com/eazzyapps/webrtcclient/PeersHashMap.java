package com.eazzyapps.webrtcclient;

import java.util.HashMap;

/**
 * Created by Александр on 06.09.2016.
 */
public class PeersHashMap extends HashMap<String, EAPeer> {

    PeersEvents observer;

    public PeersHashMap(PeersEvents observer) {
        super();
        this.observer = observer;
    }
    public interface PeersEvents {
        void onPeerAdded(EAPeer value);
        void onPeerRemoved(EAPeer value);
    }

    @Override
    public EAPeer put(String key, EAPeer peer) {
        EAPeer res = super.put(key, peer);
        observer.onPeerAdded(peer);
        return res;
    }

    @Override
    public EAPeer remove(Object key) {
        EAPeer res = super.remove(key);
        observer.onPeerRemoved(res);
        return res;
    }
}
