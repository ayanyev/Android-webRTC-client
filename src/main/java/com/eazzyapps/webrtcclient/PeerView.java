package com.eazzyapps.webrtcclient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * TODO: document your custom view class.
 */
public class PeerView extends RelativeLayout {

    public final static int LEVEL_FRAME = 0;
    public final static int LEVEL_PROGRESS = 1;
    public final static int LEVEL_READY_TO_CONNECT = 2;

    TextView peerName;
    ImageView display;
    EAPeer peer;
    int peerNameHeight;

    public PeerView(Context context, EAPeer peer) {
        this(context, peer, null, 0);
    }

    public PeerView(Context context, EAPeer peer, AttributeSet attrs) {
        this(context, peer, attrs, 0);
    }

    public PeerView(Context context, EAPeer peer, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.peer = peer;

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PeerView, defStyle, 0);

        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.peer_view, this, true);

        peerName = (TextView) findViewById(R.id.peer_name);
        display = (ImageView) findViewById(R.id.display);

        if (peer.isMyself()) {
            peerName.setText("me");
        } else {
            peerName.setText(peer.getUserName());
            setOnClickListener(listener);
        }
    }

    private OnClickListener listener = v -> {

        peer.createOffer();
        display.setImageLevel(LEVEL_PROGRESS);

    };

    public int getPeerNameHeight(){
        return peerNameHeight;
    }

    public void setImageLevel(int level){

        display.setImageLevel(level);
    }

    public void setPeerName(String name){
        peerName.setText(name);
    }

}
