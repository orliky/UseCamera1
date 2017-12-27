package com.yso.usecamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a simple View to display the faces.
 */
public class FaceOverlayView extends View
{

    private Paint mPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;
    List<Rect> mFaceRects = new ArrayList<>();

    public FaceOverlayView(Context context)
    {
        super(context);
        initialize();
    }

    private void initialize()
    {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.DKGRAY);
        mPaint.setAlpha(128);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setFaces(Face[] faces, ArrayList faceRects)
    {
//        mFaces = faces;
//        invalidate();
        mFaceRects = faceRects;
        invalidate();
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
        Matrix matrix = new Matrix();
        canvas.save();
        matrix.postRotate(mOrientation);
        canvas.rotate(-mOrientation);
        RectF rectF = new RectF();
        for(Rect rect : mFaceRects)
        {
            rectF.set(rect);
            canvas.drawRect(rectF, mPaint);
        }
        canvas.restore();
       /* if (mFaces != null && mFaces.length > 0)
        {
            Matrix matrix = new Matrix();
            canvas.save();
            matrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            RectF rectF = new RectF();
            for (Face face : mFaces)
            {
                rectF.set(face.rect);
                matrix.mapRect(rectF);
                canvas.drawRect(rectF, mPaint);
            }
            canvas.restore();
        }*/
    }
}
