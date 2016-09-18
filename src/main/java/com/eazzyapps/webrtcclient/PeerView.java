package com.eazzyapps.webrtcclient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Constructor;

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
    private Rect layoutInPercentage;

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

    // overload for resizing purpose
    public void setLayoutFromPercentage(int glWidth, int glHeight) {
        setLayoutFromPercentage(layoutInPercentage, glWidth, glHeight);
    }

    // overload for creation purpose
    public void setLayoutFromPercentage(Rect layoutInPercentage, int glWidth, int glHeight) {

        this.layoutInPercentage = layoutInPercentage;

        // derived from VideoRendererGui

        Rect displayLayout = new Rect();
        displayLayout.set(
                (glWidth * layoutInPercentage.left + 99) / 100,
                (glHeight * layoutInPercentage.top + 99) / 100,
                glWidth * layoutInPercentage.right / 100,
                glHeight * layoutInPercentage.bottom / 100);
        displayLayout.inset(-10, -10);

//        float videoAspectRatio = this.rotationDegree % 180 == 0?(float)this.videoWidth / (float)this.videoHeight:(float)this.videoHeight / (float)this.videoWidth;
//        float minVisibleFraction = convertScalingTypeToVisibleFraction(this.scalingType);
//        Point displaySize = getDisplaySize(minVisibleFraction, videoAspectRatio, this.layoutInPercentage.width(), this.layoutInPercentage.height());
//        layoutInPercentage.inset((this.layoutInPercentage.width() - displaySize.x) / 2, (this.layoutInPercentage.height() - displaySize.y) / 2);

//        try {
//            Constructor<? extends LayoutParams> ctor = this.getLayoutParams().getClass().getDeclaredConstructor(int‌​.class, int.class);
//            this.setLayoutParams(ctor.newInstance(displayLayout.right - displayLayout.left,
//                                                  displayLayout.bottom - displayLayout.top));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        RelativeLayout.LayoutParams params = (LayoutParams) getLayoutParams();

        if (params == null)
            params = new RelativeLayout.LayoutParams(
                    displayLayout.right - displayLayout.left,
                    displayLayout.bottom - displayLayout.top);
        else {
            params.width = displayLayout.right - displayLayout.left;
            params.height = displayLayout.bottom - displayLayout.top;
        }

        params.leftMargin = displayLayout.left;
        params.topMargin = displayLayout.top;

        Log.d(Constants.TAG, displayLayout.toString());

        setLayoutParams(params);
    }

    public int getPeerNameHeight() {
        return peerNameHeight;
    }

    public void setImageLevel(int level) {

        display.setImageLevel(level);
    }

    public void setPeerName(String name) {
        peerName.setText(name);
    }

    public Rect getLayoutInPercentage() {
        return layoutInPercentage;
    }

}
