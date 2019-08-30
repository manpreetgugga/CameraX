package com.example.camerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.util.Size;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private TextureView textureView;
    private RectOverlayView rectOverlayView;
    public DisplayMetrics displayMetrics;

    public ScreenOrientation currentScreenOrientation;

    public MyOrientationEventListener mOrientationListener;
    public ImageView takePicture;

    int cameraOverlayWidth;
    int cameraOverlayHeight;

    public enum ScreenOrientation {
        PORTRAIT(90),
        REVERSE_PORTRAIT(-90),
        LANDSCAPE(0),
        REVERSE_LANDSCAPE(180);
        int rotationRequired;

        ScreenOrientation(int rotationRequired) {
            this.rotationRequired = rotationRequired;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
        checkRequiredPermission();
    }

    private void initUI() {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
    }

    private void startCamera() {
        textureView = findViewById(R.id.cameraTextureView);
        takePicture = (ImageView) findViewById(R.id.takePicture);
        textureView.post(new Runnable() {
            @Override
            public void run() {
                setUpOrientationChangeListeners();
                handleStartOperations();
                updateScannerView();
            }
        });
    }

    private void updateScannerView() {
        Display display = textureView.getDisplay();
        if (display != null) {
            displayMetrics = new DisplayMetrics();
            Display textureDisplay = textureView.getDisplay();
            textureDisplay.getRealMetrics(displayMetrics);
            FrameLayout myLayout = findViewById(R.id.textureContainer);
            if (rectOverlayView != null) {
                myLayout.removeView(rectOverlayView);
            }
            boolean isPortrait = false;

            if (currentScreenOrientation == ScreenOrientation.PORTRAIT) {
                cameraOverlayWidth = displayMetrics.widthPixels - (displayMetrics.widthPixels / 6);
                cameraOverlayHeight = (displayMetrics.heightPixels / 4);
                isPortrait = true;
            } else {
                cameraOverlayWidth = displayMetrics.widthPixels - (displayMetrics.widthPixels / 6);
                cameraOverlayHeight = displayMetrics.heightPixels - (displayMetrics.heightPixels / 4);
                isPortrait = false;
            }

            rectOverlayView = new RectOverlayView(isPortrait, cameraOverlayWidth, cameraOverlayHeight, this);

            rectOverlayView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            myLayout.addView(rectOverlayView);
        }
    }

    private void handleStartOperations() {
        int width = textureView.getWidth() >= 1080 ? 1080 : textureView.getWidth();
        int height = textureView.getHeight() >= 1920 ? 1920 : textureView.getHeight();

        Rational aspectRatio = new Rational(width, height);
        Size screen = new Size(width, height);

        PreviewConfig pConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screen)
                .build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetRotation(textureView.getDisplay().getRotation())
                .setTargetResolution(screen)
                .setFlashMode(FlashMode.AUTO)
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .build();

        final ImageCapture imageCapture = new ImageCapture(imageCaptureConfig);
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentScreenOrientation == ScreenOrientation.REVERSE_LANDSCAPE || currentScreenOrientation == ScreenOrientation.LANDSCAPE) {
                    imageCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                        @Override
                        public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                            handleOnCameraCaptureSuccess(image);
                            super.onCaptureSuccess(image, rotationDegrees);
                        }

                        @Override
                        public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                            super.onError(useCaseError, message, cause);
                        }
                    });
                }
            }
        });

        // might get used in Future
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis);
    }

    public void handleOnCameraCaptureSuccess(ImageProxy image) {
        Bitmap imageCaptured = imageToBitmap(image);
        imageCaptured = rotateBitmap(imageCaptured);
        final Bitmap finalImageCaptured = imageCaptured;
        Bitmap centerCropedImage = getCenterBitmap(finalImageCaptured);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        DialogFragment dialogFragment = ImageViewCustomDialogFragment.newInstance(centerCropedImage);
        dialogFragment.setCancelable(true);
        dialogFragment.show(ft, "dialog");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                checkRequiredPermission();
            }
        }
    }

    private void updateTransform() {
        Matrix mx = new Matrix();

        int width = textureView.getWidth() >= 1080 ? 1080 : textureView.getWidth();
        int height = textureView.getHeight() >= 1920 ? 1920 : textureView.getHeight();

        float w = width;
        float h = height;

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    Bitmap imageToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 6;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public Bitmap getCenterBitmap(Bitmap bitmap) {
        if (currentScreenOrientation == ScreenOrientation.REVERSE_LANDSCAPE) {
            currentScreenOrientation = ScreenOrientation.REVERSE_PORTRAIT;
            bitmap = rotateBitmap(bitmap);
            currentScreenOrientation = ScreenOrientation.REVERSE_LANDSCAPE;
        }
        if (currentScreenOrientation == ScreenOrientation.LANDSCAPE) {
            currentScreenOrientation = ScreenOrientation.PORTRAIT;
            bitmap = rotateBitmap(bitmap);
            currentScreenOrientation = ScreenOrientation.LANDSCAPE;
        }

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();

        Point centerOfCanvas = new Point(bitmap.getWidth() / 2, bitmap.getHeight() / 2);

        int rectW = dpToPx(rectOverlayView.cameraOverLayWidth);
        int rectH = dpToPx(rectOverlayView.cameraOverLayHeight);

        int left = centerOfCanvas.x - (rectW / 2);
        int top = centerOfCanvas.y - (rectH / 2);
        int right = centerOfCanvas.x + (rectW / 2);
        int bottom = centerOfCanvas.y + (rectH / 2);
        Rect rect = new Rect(left, top, right, bottom);


//        Rect rect = new Rect(rectOverlayView.rect.left - (rectOverlayView.rect.left / 4),
//                rectOverlayView.rect.top - (rectOverlayView.rect.top / 6),
//                dpToPx(rectOverlayView.rect.right) - dpToPx((rectOverlayView.rect.right / 4)),
//                dpToPx(rectOverlayView.rect.bottom) - dpToPx((rectOverlayView.rect.bottom / 6)));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        if (currentScreenOrientation == ScreenOrientation.REVERSE_LANDSCAPE) {
            currentScreenOrientation = ScreenOrientation.PORTRAIT;
            output = rotateBitmap(output);
            currentScreenOrientation = ScreenOrientation.REVERSE_LANDSCAPE;
        }
        if (currentScreenOrientation == ScreenOrientation.LANDSCAPE) {
            currentScreenOrientation = ScreenOrientation.REVERSE_PORTRAIT;
            output = rotateBitmap(output);
            currentScreenOrientation = ScreenOrientation.LANDSCAPE;
        }
        return output;
    }

    public Bitmap rotateBitmap(Bitmap source) {
        float angle = currentScreenOrientation.rotationRequired;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation <= 45) {
                currentScreenOrientation = ScreenOrientation.PORTRAIT;
            } else if (orientation <= 135) {
                currentScreenOrientation = ScreenOrientation.REVERSE_LANDSCAPE;
            } else if (orientation <= 225) {
                currentScreenOrientation = ScreenOrientation.REVERSE_PORTRAIT;
            } else if (orientation <= 315) {
                currentScreenOrientation = ScreenOrientation.LANDSCAPE;
            } else {
                currentScreenOrientation = ScreenOrientation.PORTRAIT;
            }

            textureView.post(new Runnable() {
                @Override
                public void run() {
                    updateScannerView();
                }
            });
        }
    }


    private void setUpOrientationChangeListeners() {
        mOrientationListener = new MyOrientationEventListener(this);
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    private void checkRequiredPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        MY_CAMERA_REQUEST_CODE);
            } else {
                startCamera();
            }
        } else {
            startCamera();
        }
    }

    enum dimens {
        LDPI(0.75f),
        MDPI(1.0f),
        HDPI(1.5f),
        XHDPI(2.0f),
        XXDPI(3.0f),
        XXXDPI(4.75f);

        dimens(float v) {
        }
    }

    private int dpToPx(int dp) {
        if (getResources().getDisplayMetrics().density <= 2.0f) {
            return Math.round(dp * getResources().getDisplayMetrics().density);
        }
        return dp;
    }
}