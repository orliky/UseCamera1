package com.yso.usecamera;

import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MyFaceDetectionListener implements Camera.FaceDetectionListener
{
    private static final String TAG = MyFaceDetectionListener.class.getSimpleName();

    private FaceOverlayView mFaceView;


    public MyFaceDetectionListener(FaceOverlayView faceOverlayView)
    {
        mFaceView = faceOverlayView;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera)
    {
        //        if (faces.length == 0)
        //        {
        //            Log.i(TAG, "No faces detected");
        //        }
        //        else if (faces.length > 0)
        //        {
        Log.i(TAG, "Faces Detected = " + String.valueOf(faces.length));
        //        }
        setRect(faces);
    }

    public void setRect(Camera.Face[] faces)
    {
        mFaceView.setFaces(faces);
    }
}
