package com.example.camerax;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.fonts.Font;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class RectOverlayView extends LinearLayout {
    private Bitmap bitmap;
    private boolean isPotrait;
    public Canvas osCanvas;
    public Rect centerCropRect;

    public RectOverlayView(boolean isPortrait,int width, int height ,Context context) {
        super(context);
        this.cameraOverLayWidth = width;
        this.cameraOverLayHeight = height;
        this.isPotrait = isPortrait;
    }

    public RectOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RectOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (bitmap == null) {
            createWindowFrame();
        }
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    public int cameraOverLayWidth, cameraOverLayHeight;
    public int canvasWidth, canvasHeight;

    public Rect rect;


    protected void createWindowFrame() {
        canvasWidth = getWidth();
        canvasHeight = getHeight();

        bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
        Canvas osCanvas = new Canvas(bitmap);

        RectF outerRectangle = new RectF(0, 0, canvasWidth, canvasHeight);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.colorPrimary));
        paint.setAlpha(99);
        osCanvas.drawRect(outerRectangle, paint);

        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));

        Point centerOfCanvas = new Point(canvasWidth / 2, canvasHeight / 2);

        int rectW = cameraOverLayWidth;
        int rectH = cameraOverLayHeight;

        int left = centerOfCanvas.x - (rectW / 2);
        int top = centerOfCanvas.y - (rectH / 2);
        int right = centerOfCanvas.x + (rectW / 2);
        int bottom = centerOfCanvas.y + (rectH / 2);
        rect = new Rect(left, top, right, bottom);

        osCanvas.drawRect(rect, paint);

        if(isPotrait){
            Paint textPaint = new Paint();
            textPaint.setARGB(200, 254, 0, 0);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(48);
            osCanvas.drawText("Please rotate to Portrait Mode", centerOfCanvas.x, centerOfCanvas.y, textPaint);
        }
    }

    @Override
    public boolean isInEditMode() {
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bitmap = null;
    }
}