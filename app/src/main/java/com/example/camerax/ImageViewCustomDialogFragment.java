package com.example.camerax;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ImageViewCustomDialogFragment extends DialogFragment {

    Bitmap imageToShow;

    public static ImageViewCustomDialogFragment newInstance(Bitmap imageToShow) {
        ImageViewCustomDialogFragment fragment = new ImageViewCustomDialogFragment();
        fragment.imageToShow = imageToShow;
        return fragment;
    }

    public ImageViewCustomDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.custom_image_view, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ImageView) view.findViewById(R.id.caputuredImage)).post(new Runnable() {
            @Override
            public void run() {
                ((ImageView) view.findViewById(R.id.caputuredImage)).setImageBitmap(imageToShow);
            }
        });
    }
}
