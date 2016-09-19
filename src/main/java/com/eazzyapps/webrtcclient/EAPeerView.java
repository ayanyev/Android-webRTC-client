package com.eazzyapps.webrtcclient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.webrtc.VideoRenderer;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action;

/**
 * TODO: document your custom view class.
 */
public class EAPeerView extends RelativeLayout {

    public final static int LEVEL_FRAME = 0;
    public final static int LEVEL_PROGRESS = 1;
    public final static int LEVEL_READY_TO_CONNECT = 2;

    TextView peerName;
    ImageView display;
    EAPeer peer;
    private VideoRenderer renderer;
    private Rect layoutInPercentage;
    private Rect displayLayout;
    private boolean paramsToBeChanged;
    private ViewTreeObserver.OnGlobalLayoutListener layoutPassCompletedListener = () -> {

        if (paramsToBeChanged) {

            paramsToBeChanged = false;
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

            setLayoutParams(params);

            Log.d(Constants.TAG, displayLayout.toString());
        }
    };
    private OnClickListener viewClickListener = v -> {

        peer.createOffer();
        display.setImageLevel(LEVEL_PROGRESS);

    };

    public EAPeerView(Context context, EAPeer peer) {
        this(context, peer, null, 0);
    }

    public EAPeerView(Context context, EAPeer peer, AttributeSet attrs) {
        this(context, peer, attrs, 0);
    }

    public EAPeerView(Context context, EAPeer peer, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.peer = peer;
        this.paramsToBeChanged = false;

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.EAPeerView, defStyle, 0);

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
            setOnClickListener(viewClickListener);
        }

        // to listen parent's layout pass completed
        // to set new params and start new layout pass
        getRootView()
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(layoutPassCompletedListener);
    }

    // overload for creation purpose
    public void setSizeAndPosition(int glWidth, int glHeight) {

        if (layoutInPercentage == null)
            throw new NullPointerException("Layout parameters in % cannot be null");

        // derived from VideoRendererGui

        displayLayout = new Rect();
        displayLayout.set(
                (glWidth * layoutInPercentage.left + 99) / 100,
                (glHeight * layoutInPercentage.top + 99) / 100,
                glWidth * layoutInPercentage.right / 100,
                glHeight * layoutInPercentage.bottom / 100);
        displayLayout.inset(-10, -10);

        paramsToBeChanged = true;

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
    }

    public void setImageLevel(int level) {
        Observable.just(level)
                .doOnNext(l -> display.setImageLevel(l))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void setPeerName(String name) {
        Observable.just(name)
                .doOnNext(n -> peerName.setText(n))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void setLayoutInPercentage(int left, int top, int width, int height) {
        this.layoutInPercentage = new Rect(left, top, Math.min(100, left + width), Math.min(100, top + height));
    }

    public VideoRenderer getRenderer() {
        return renderer;
    }

    public void setRenderer(VideoRenderer renderer) {
        this.renderer = renderer;
    }

//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//        renderer.dispose();
//    }
}
