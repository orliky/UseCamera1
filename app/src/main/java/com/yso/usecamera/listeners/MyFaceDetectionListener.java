package com.yso.usecamera.listeners;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import com.yso.usecamera.views.FaceOverlayView;

public class MyFaceDetectionListener implements Camera.FaceDetectionListener
{
    private static final String TAG = MyFaceDetectionListener.class.getSimpleName();

    private Context mContext;

    public MyFaceDetectionListener(Context context)
    {
        mContext = context;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera)
    {
        Log.i(TAG, "Faces Detected = " + String.valueOf(faces.length));
        setRect(faces);
    }

    private void setRect(Camera.Face[] faces)
    {
        FaceOverlayView.getInstance(mContext).setFaces(faces);
    }
}
