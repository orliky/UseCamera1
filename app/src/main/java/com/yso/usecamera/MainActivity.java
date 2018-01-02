package com.yso.usecamera;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.yso.usecamera.managers.SharedPref;
import com.yso.usecamera.utils.FlipAnimation;
import com.yso.usecamera.views.CameraView;
import com.yso.usecamera.views.FaceOverlayView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int MY_STORAGE_REQUEST_CODE = 101;
    int TAKE_PHOTO_CODE = 0;

    private CameraView mCameraView = null;
    private FrameLayout mCameraLayout;
    private FaceOverlayView mFaceView;
    private String mDir;
    private boolean isInit;
    private ImageView mPreviewImage;
    private ImageView mPreviewSmallImage;
    private FrameLayout mGroup;
    private View mOpacityFilter;
    private Bitmap mLastPic;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/UseCamera/";
        File newdir = new File(mDir);
        newdir.mkdirs();

        SharedPref.init(getApplicationContext());
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
            mGroup.removeView(mFaceView);
            mCameraView.stopCamera(mCameraLayout);
        }
    }

    private void init()
    {
        isInit = true;

        mFaceView = FaceOverlayView.getInstance(this);
        mGroup = findViewById(R.id.group);
        mGroup.addView(mFaceView);

        mCameraView = new CameraView(MainActivity.this);
        mCameraLayout = findViewById(R.id.camera_view);
        mCameraLayout.addView(mCameraView);
        mPreviewImage = findViewById(R.id.previewImage);
        mPreviewSmallImage = findViewById(R.id.previewSmallImage);
        mPreviewSmallImage.setEnabled(false);
        mPreviewSmallImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openImage();
            }
        });
        mOpacityFilter = findViewById(R.id.opacityFilter);

        ImageButton imgClose = findViewById(R.id.imgClose);
        imgClose.setOnClickListener(this);

        ImageButton imgChange = findViewById(R.id.imgChange);
        imgChange.setOnClickListener(this);

        ImageButton imgCapture = findViewById(R.id.imgCapture);
        imgCapture.setOnClickListener(this);
    }

    private void openImage()
    {
        final Dialog nagDialog = new Dialog(MainActivity.this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setContentView(R.layout.preview_image);
        ImageView ivPreview = nagDialog.findViewById(R.id.iv_preview_image);
        ImageView closePreview = nagDialog.findViewById(R.id.close_preview);
        closePreview.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                nagDialog.dismiss();
            }
        });
        ivPreview.setImageBitmap(mLastPic);
        nagDialog.getWindow().setBackgroundDrawable(null);
        nagDialog.show();

        addFaceDetector();
    }

    private void addFaceDetector()
    {
        FaceDetector detector = new FaceDetector.Builder(MainActivity.this).setTrackingEnabled(false).setLandmarkType(FaceDetector.ALL_LANDMARKS).build();
        Frame frame = new Frame.Builder().setBitmap(mLastPic).build();
        SparseArray<Face> faces = detector.detect(frame);
        for (int i = 0; i < faces.size(); ++i)
        {
            com.google.android.gms.vision.face.Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks())
            {
                int cx = (int) (landmark.getPosition().x);
                int cy = (int) (landmark.getPosition().y);
                Canvas canvas = new Canvas(mLastPic);
                Paint p = new Paint();
                p.setColor(Color.GREEN);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(10);
                canvas.drawCircle(cx, cy, 15, p);
                mPreviewSmallImage.draw(canvas);
            }
        }
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

    private void takePic()
    {
        // flicker animation
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1);
        anim.setStartOffset(10);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        mCameraLayout.startAnimation(anim);
        ///////////////////////////
        //create image file
        String file = mDir + System.currentTimeMillis() + ".jpg";
        final File newfile = new File(file);
        try
        {
            newfile.createNewFile();
        } catch (IOException e)
        {
            Toast.makeText(MainActivity.this, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
        }
        //rotate camera
        setRotationParameter(MainActivity.this, mCameraView.getCurrentCameraId(), mCameraView.getCamera().getParameters());
        //take the pic
        mCameraView.getCamera().takePicture(null, null, getPictureCallback(newfile));
    }

    @NonNull
    private Camera.PictureCallback getPictureCallback(final File newfile)
    {
        return new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera)
            {
                if (!newfile.exists() && !newfile.mkdirs())
                {
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

                    mPreviewImage.setBackgroundResource(0);
                    //rotate pic in gallery
                    Bitmap myBitmap = BitmapFactory.decodeFile(newfile.getAbsolutePath());
                    ExifInterface exif = new ExifInterface(pictureFile.toString());
                    myBitmap = rotateBitmap(myBitmap, exif);

                    mPreviewImage.setImageBitmap(myBitmap);
                    mPreviewSmallImage.setImageBitmap(myBitmap);
                    mPreviewSmallImage.setEnabled(true);
                    mLastPic = myBitmap;
                    //preview animation
                    mPreviewImage.setVisibility(View.VISIBLE);
                    Animation connectingAnimation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.alpha_scale_animation);
                    mPreviewImage.startAnimation(connectingAnimation);

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
                    mPreviewImage.setVisibility(View.GONE);
                    mPreviewImage.setBackgroundResource(0);
                    Toast.makeText(MainActivity.this, "Image could not be saved.", Toast.LENGTH_LONG).show();
                }
            }
        };
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
        mCameraView.getCamera().setParameters(param);
    }

    private Bitmap rotateBitmap(Bitmap myBitmap, ExifInterface exif)
    {
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

        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.imgClose:
                finish();
                break;

            case R.id.imgChange:
                FlipAnimation flipAnimation = FlipAnimation.create(FlipAnimation.LEFT, true, 400);
                mCameraView.startAnimation(flipAnimation);
                flipAnimation.setAnimationListener(new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {
                        mOpacityFilter.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        mOpacityFilter.setVisibility(View.GONE);
                        mCameraView.changeCamera();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                });
                break;

            case R.id.imgCapture:
                takePic();
                break;
        }
    }
}
