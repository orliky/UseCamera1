package com.yso.usecamera.views;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.yso.usecamera.listeners.MyFaceDetectionListener;
import com.yso.usecamera.managers.SharedPref;

import java.io.IOException;
import java.util.Objects;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = CameraView.class.getSimpleName();

    private Camera mCamera;
    private int mCurrentCameraId;
    private boolean mFaceDetectionRunning;
    private FaceOverlayView mFaceView;
    private Context mContext;

    public CameraView(Context context)
    {
        super(context);

        mCurrentCameraId = SharedPref.read(SharedPref.CAMERA_ID, -1);

        try
        {
            if (mCurrentCameraId == -1)
            {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            else
            {
                mCamera = Camera.open(mCurrentCameraId);
            }
        } catch (Exception e)
        {
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }
        SurfaceHolder surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        //        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mFaceView = FaceOverlayView.getInstance(context);
        mFaceView.setIsFrontCamera(mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT);
        mContext = context;
    }

    public Camera getCamera()
    {
        return mCamera;
    }

    public int getCurrentCameraId()
    {
        return mCurrentCameraId;
    }

    public void stopCamera(FrameLayout frameLayout)
    {
        mFaceView.setFaces(null);
        surfaceDestroyed(getHolder());
        getHolder().removeCallback(this);
        destroyDrawingCache();
        frameLayout.removeView(this);
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
    }

    public void changeCamera()
    {
        stopFaceDetection();
        surfaceDestroyed(getHolder());
        mCamera.stopPreview();
        mCamera.release();

        if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
        {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            SharedPref.write(SharedPref.CAMERA_ID, mCurrentCameraId);
            mFaceView.setIsFrontCamera(true);
        }
        else
        {
            mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            SharedPref.write(SharedPref.CAMERA_ID, mCurrentCameraId);
            mFaceView.setIsFrontCamera(false);
        }
        mCamera = Camera.open(mCurrentCameraId);

        setCameraDisplayOrientation((Activity) mContext, mCurrentCameraId, mCamera);
        try
        {
            mCamera.setPreviewDisplay(getHolder());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        mCamera.startPreview();
        surfaceCreated(getHolder());
        doFaceDetection();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            setCameraDisplayOrientation((Activity) mContext, mCurrentCameraId, mCamera);

            mCamera.setPreviewDisplay(holder);

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

    public void doFaceDetection()
    {
        if (mFaceDetectionRunning)
        {
            return;
        }

        if (mCamera.getParameters().getMaxNumDetectedFaces() <= 0) {
            Log.e(TAG, "Face Detection not supported");
            return;
        }

        MyFaceDetectionListener fDListener = new MyFaceDetectionListener(mContext);
        mCamera.setFaceDetectionListener(fDListener);
        mCamera.startFaceDetection();
        mFaceDetectionRunning = true;
    }

    public void stopFaceDetection()
    {
        if (mFaceDetectionRunning)
        {
            mCamera.stopFaceDetection();
            mFaceDetectionRunning = false;
        }
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
            result = (360 - result) % 360;
        }
        else
        {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        mFaceView.setDisplayOrientation(result);
    }
}