package com.yso.usecamera;

import android.hardware.Camera;
import android.util.Log;

public class MyFaceDetectionListener implements Camera.FaceDetectionListener
{
    private static final String TAG = MyFaceDetectionListener.class.getSimpleName();
    private FaceOverlayView mFaceView;


    MyFaceDetectionListener(FaceOverlayView faceOverlayView)
    {
        mFaceView = faceOverlayView;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera)
    {
        Log.i(TAG, "Faces Detected = " + String.valueOf(faces.length));
        setRect(faces);
    }

    private void setRect(Camera.Face[] faces)
    {
        mFaceView.setFaces(faces);
    }
}
