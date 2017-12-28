package com.yso.usecamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.support.v4.content.FileProvider.getUriForFile;

public class MainActivity extends AppCompatActivity
{
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_STORAGE_REQUEST_CODE = 101;
    int TAKE_PHOTO_CODE = 0;

    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private FrameLayout mCameraLayout;
    private int mCurrentCameraId;
    private FaceOverlayView mFaceView;
    private String mDir;
    private boolean isInit;
    private ImageView mPreviewImage;
    private ProgressBar mProgressBar;
    private FrameLayout background;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/UseCamera/";
        File newdir = new File(mDir);
        newdir.mkdirs();
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

        if (isInit)
        {
            mCameraView.surfaceDestroyed(mCameraView.getHolder());
            mCameraView.getHolder().removeCallback(mCameraView);
            mCameraView.destroyDrawingCache();
            mCameraLayout.removeView(mCameraView);
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
        }
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
            isInit = true;

            mFaceView = new FaceOverlayView(this);
            addContentView(mFaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            mCameraView = new CameraView(this, mCamera, mCurrentCameraId, mFaceView);//create a SurfaceView to show camera data
            mCameraLayout = findViewById(R.id.camera_view);
            mCameraLayout.addView(mCameraView);//add the SurfaceView to the layout
        }

        mPreviewImage = findViewById(R.id.previewImage);
        mProgressBar = findViewById(R.id.progress);
        background = findViewById(R.id.background);

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

        ImageButton imgCapture = findViewById(R.id.imgCapture);
        imgCapture.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                background.setVisibility(View.VISIBLE);
                Handler handler = new Handler();
                Runnable run = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        background.setVisibility(View.GONE);
                    }
                };
                handler.postDelayed(run, 200);

//                mProgressBar.setVisibility(View.VISIBLE);
                mPreviewImage.setVisibility(View.VISIBLE);
               /* mPreviewImage.setBackgroundResource(R.drawable.anim);
                AnimationDrawable anim = (AnimationDrawable) mPreviewImage.getBackground();
                anim.start();*/

                Animation connectingAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.alpha_scale_animation);
                mPreviewImage.startAnimation(connectingAnimation);

                String file = mDir + System.currentTimeMillis() + ".jpg";
                final File newfile = new File(file);
                try
                {
                    newfile.createNewFile();
                } catch (IOException e)
                {
                }
                setRotationParameter(MainActivity.this, mCurrentCameraId, mCamera.getParameters());
                mCamera.takePicture(null, null, new Camera.PictureCallback()
                {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera)
                    {
                        if (!newfile.exists() && !newfile.mkdirs())
                        {
                            mProgressBar.setVisibility(View.GONE);
                            mPreviewImage.setVisibility(View.GONE);
                            mPreviewImage.setBackgroundResource(0);
                            Toast.makeText(MainActivity.this, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String filename = newfile.getPath();
                        File pictureFile = new File(filename);

                        try
                        {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();

                            mProgressBar.setVisibility(View.GONE);
                            mPreviewImage.setBackgroundResource(0);
                            Bitmap myBitmap = BitmapFactory.decodeFile(newfile.getAbsolutePath());
                            ExifInterface exif = new ExifInterface(pictureFile.toString());
                            myBitmap = rotateBitmap(myBitmap, exif);
                            mPreviewImage.setImageBitmap(myBitmap);

                            Handler handler = new Handler();
                            Runnable run = new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    mPreviewImage.setVisibility(View.GONE);
                                    mPreviewImage.setImageBitmap(null);
                                }
                            };
                            handler.postDelayed(run, 2000);

                        } catch (Exception error)
                        {
                            mProgressBar.setVisibility(View.GONE);
                            mPreviewImage.setVisibility(View.GONE);
                            mPreviewImage.setBackgroundResource(0);
                            Toast.makeText(MainActivity.this, "Image could not be saved.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @RequiresApi (api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_STORAGE_REQUEST_CODE);
                }
                else
                {
                    init();
                }
            }
            else
            {
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK)
        {
            Log.d("Capture", "Pic saved");
        }
    }

    public void setRotationParameter(Activity activity, int cameraId, Camera.Parameters param)
    {

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();


        switch (rotation)
        {
            case Surface.ROTATION_0:
                setRotate(param, info, rotation);
                break;
            case Surface.ROTATION_90:

                break;
            case Surface.ROTATION_180:
                setRotate(param, info, rotation);
                break;
            case Surface.ROTATION_270:

                break;
        }
    }

    private void setRotate(Camera.Parameters param, Camera.CameraInfo info, int rotation)
    {
        rotation = (rotation + 45) / 90 * 90;
        int toRotate = (info.orientation + rotation) % 360;
        param.setRotation(toRotate);
        mCamera.setParameters(param);
    }

    private Bitmap rotateBitmap(Bitmap myBitmap, ExifInterface exif)
    {
        Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
        if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6"))
        {
            myBitmap = rotate(myBitmap, 90);
        }
        else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8"))
        {
            myBitmap = rotate(myBitmap, 270);
        }
        else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3"))
        {
            myBitmap = rotate(myBitmap, 180);
        }
        else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0"))
        {
            myBitmap = rotate(myBitmap, 90);
        }
        return myBitmap;
    }

    public static Bitmap rotate(Bitmap bitmap, int degree)
    {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }
}
