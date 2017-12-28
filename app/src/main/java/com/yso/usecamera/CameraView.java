package com.yso.usecamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Objects;


@SuppressLint ("ViewConstructor")
public class CameraView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = CameraView.class.getSimpleName();

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private int mCurrentCameraId;
    private boolean mFaceDetectionRunning;
    private FaceOverlayView mFaceView;
    private Context mContext;

    public CameraView(Context context, Camera camera, int currentCameraId, FaceOverlayView faceOverlayView)
    {
        super(context);
        this.mCamera = camera;
        this.mSurfaceHolder = this.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.mCurrentCameraId = currentCameraId;
        mFaceView = faceOverlayView;
        mContext = context;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            setCameraDisplayOrientation((Activity) mContext, mCurrentCameraId, mCamera);

            mCamera.setPreviewDisplay(holder);

            //            mCamera.setDisplayOrientation(90);
            //            mFaceView.setDisplayOrientation(90);

            Camera.Parameters params = mCamera.getParameters();
            if (params.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_HDR))
            {
                params.setSceneMode(Camera.Parameters.SCENE_MODE_HDR);
            }

            if (params.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_NIGHT))
            {
                params.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
            }

            mCamera.cancelAutoFocus();
            if (!Objects.equals(params.getFocusMode(), Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                mCamera.setParameters(params);
            }
            mCamera.startPreview();

            doFaceDetection();

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        try
        {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    public int doFaceDetection()
    {
        if (mFaceDetectionRunning)
        {
            return 0;
        }
       /* // check if face detection is supported or not
        // using Camera.Parameters
        if (Camera.Parameters.getMaxDetectedFaces() <= 0) {
            Log.e(TAG, "Face Detection not supported");
            return -1;
        }*/

        MyFaceDetectionListener fDListener = new MyFaceDetectionListener(mFaceView);
        mCamera.setFaceDetectionListener(fDListener);
        mCamera.startFaceDetection();
        mFaceDetectionRunning = true;
        return 1;
    }

    public int stopFaceDetection()
    {
        if (mFaceDetectionRunning)
        {
            mCamera.stopFaceDetection();
            mFaceDetectionRunning = false;
            return 1;
        }
        return 0;
    }

    public void setCurrentCameraId(int currentCameraId)
    {
        mCurrentCameraId = currentCameraId;
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera)
    {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        }
        else
        { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        mFaceView.setDisplayOrientation(result);
    }
}