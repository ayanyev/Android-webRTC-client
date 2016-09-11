package com.eazzyapps.webrtcclient;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * TODO: document your custom view class.
 */
public class PeerView extends RelativeLayout {

    TextView peerName;
    int peerNameHeight;

    public PeerView(Context context) {
        this(context, null, 0);
    }

    public PeerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.PeerView, defStyle, 0);

        a.recycle();

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.peer_view, this, true);

        peerName = (TextView) findViewById(R.id.peer_name);
    }

    public int getPeerNameHeight(){
        return peerNameHeight;
    }

    public void setPeerName(String name){
        peerName.setText(name);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }
}
