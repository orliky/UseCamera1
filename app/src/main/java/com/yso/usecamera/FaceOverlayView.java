package com.yso.usecamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.view.View;


public class FaceOverlayView extends View
{

    private Paint mPaint;
    private Paint mLeftEyePaint;
    private Paint mRightEyePaint;
    private Paint mMouthEyePaint;
    private Paint mCenterPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;
    private boolean isFrontCamera;
    private static FaceOverlayView mInstance = null;

    public static FaceOverlayView getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FaceOverlayView(context);
        }
        return mInstance;
    }


    public FaceOverlayView(Context context)
    {
        super(context);
        initialize();
    }

    private void initialize()
    {
        mPaint = new Paint();
        //        mPaint.setAntiAlias(true);
        //        mPaint.setDither(true);
        mPaint.setColor(Color.DKGRAY);
        //        mPaint.setAlpha(128);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);

        mLeftEyePaint = new Paint();
        mLeftEyePaint.setColor(Color.DKGRAY);

        mRightEyePaint = new Paint();
        mRightEyePaint.setColor(Color.DKGRAY);

        mMouthEyePaint = new Paint();
        mMouthEyePaint.setColor(Color.DKGRAY);

        mCenterPaint = new Paint();
        mCenterPaint.setColor(Color.DKGRAY);
    }

    public void setFaces(Face[] faces)
    {
        mFaces = faces;
        invalidate();
    }

    public void setIsFrontCamera(boolean frontCamera)
    {
        isFrontCamera = frontCamera;
    }

    public void setOrientation(int orientation)
    {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation)
    {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if (mFaces != null)
        {
            @SuppressLint ("DrawAllocation") Matrix matrix = new Matrix();
            prepareMatrix(matrix, isFrontCamera, mDisplayOrientation, getWidth(), getHeight());
            canvas.save();
            matrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            @SuppressLint ("DrawAllocation") RectF rectF = new RectF();
            for (Face face : mFaces)
            {
                rectF.set(face.rect);
                matrix.mapRect(rectF);
                canvas.drawRect(rectF, mPaint);

                canvas.drawCircle(rectF.centerX(), rectF.centerY(), 10f, mCenterPaint);

                if (face.leftEye != null)
                {
                    float x1 = face.leftEye.x;
                    float y1 = face.leftEye.y;
                    canvas.drawCircle(x1, y1, 10f, mLeftEyePaint);
                }

                if (face.rightEye != null)
                {
                    float x2 = face.rightEye.x;
                    float y2 = face.rightEye.y;
                    canvas.drawCircle(x2, y2, 10f, mRightEyePaint);

                }

                if (face.mouth != null)
                {
                    float x3 = face.mouth.x;
                    float y3 = face.mouth.y;
                    canvas.drawCircle(x3, y3, 10f, mMouthEyePaint);
                }
            }
            canvas.restore();
        }
        else
        {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }

    public void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth, int viewHeight)
    {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

}
