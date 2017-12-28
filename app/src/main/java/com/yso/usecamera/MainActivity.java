package com.yso.usecamera;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private FrameLayout mCameraLayout;
    private int mCurrentCameraId;
    private FaceOverlayView mFaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @RequiresApi (api = Build.VERSION_CODES.M)
    @Override
    protected void onResume()
    {
        super.onResume();

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        else
        {
            init();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        mCameraView.surfaceDestroyed(mCameraView.getHolder());
        mCameraView.getHolder().removeCallback(mCameraView);
        mCameraView.destroyDrawingCache();
        mCameraLayout.removeView(mCameraView);
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
    }

    private void init()
    {
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

        if (mCamera != null)
        {
            mFaceView = new FaceOverlayView(this);
            addContentView(mFaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            mCameraView = new CameraView(this, mCamera, mCurrentCameraId, mFaceView);//create a SurfaceView to show camera data
            mCameraLayout = findViewById(R.id.camera_view);
            mCameraLayout.addView(mCameraView);//add the SurfaceView to the layout
        }

        ImageButton imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                finish();
            }
        });

        ImageButton imgChange = findViewById(R.id.imgChange);
        imgChange.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mCameraView.surfaceDestroyed(mCameraView.getHolder());
                mCameraView.getHolder().removeCallback(mCameraView);
                mCameraView.destroyDrawingCache();
                mCameraLayout.removeView(mCameraView);
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();

                if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                {
                    mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    mFaceView.setIsFrontCamera(true);
                }
                else
                {
                    mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                    mFaceView.setIsFrontCamera(false);
                }
                mCamera = Camera.open(mCurrentCameraId);
                mCameraView = new CameraView(MainActivity.this, mCamera, mCurrentCameraId, mFaceView);
                mCameraLayout.addView(mCameraView);
                try
                {
                    mCamera.setPreviewDisplay(mCameraView.getHolder());
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                mCamera.startPreview();
                mCameraView.setCurrentCameraId(mCurrentCameraId);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                init();
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
