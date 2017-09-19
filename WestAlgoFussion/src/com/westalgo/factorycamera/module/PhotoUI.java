
package com.westalgo.factorycamera.module;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.FocusOverlayManager.FocusUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.ui.PreviewStatusListener;
import com.westalgo.factorycamera.util.CameraUtil;

public class PhotoUI implements TextureViewListener, PreviewStatusListener {

    private static final Log.Tag TAG = new Log.Tag("PhotoUI");
    private CameraActivity mActivity;
    private PhotoController mController;
    private FrameLayout mLayout;
    // textureView
    private TextureViewHelper mTextureViewHelper;
    private TextureView mTextureView = null;
    private SurfaceTexture mSurfaceTexture = null;
    // Just update main TextureView layout
    private float mAspectRatio = TextureViewHelper.MATCH_SCREEN;
    private int mPreviewWidth = -1;
    private int mPreviewHeight = -1;

    private FocusUI mFocusUI;

    private final GestureDetector.OnGestureListener mPreviewGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            mController.onSingleTapUp((int) ev.getX(), (int) ev.getY());
            return true;
        }
    };

    private OnLayoutChangeListener mLayoutListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right,
                int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

            int width = right - left;
            int height = bottom - top;
            Log.e(TAG, "onLayoutChange width :" + width + " :height: " + height);
            if (mPreviewWidth != width || mPreviewHeight != height) {
                mPreviewWidth = width;
                mPreviewHeight = height;
            }
        }
    };

    public PhotoUI(CameraActivity activity, PhotoController controller, FrameLayout parent) {
        mActivity = activity;
        mController = controller;
        mLayout = (FrameLayout) parent.findViewById(R.id.texture_view_root);
        mLayout.removeAllViews();

        mFocusUI = (FocusUI) parent.findViewById(R.id.focus_overlay);

        mActivity.getLayoutInflater().inflate(R.layout.photo_module, (ViewGroup) mLayout, true);
        mTextureView = (TextureView) mLayout.findViewById(R.id.preview_content);
        mTextureViewHelper = new TextureViewHelper(mTextureView);
        mTextureViewHelper.setSurfaceTextureListener(this);

        mTextureView.addOnLayoutChangeListener(mLayoutListener);
    }

    public SurfaceTexture getMainSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setDisplayOrientation(int ori) {

    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return mPreviewGestureListener;
    }

    public FocusUI getFocusUI() {
        return mFocusUI;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.v(TAG, "onSurfaceTextureAvailable width " + width + " height " + height);
        mSurfaceTexture = surface;
        mController.onPreviewUIReady(true);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.v(TAG, "onPreviewUIDestroyed");
        mController.onPreviewUIDestroyed(true);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        mSurfaceTexture = surface;
        mActivity.hideModeCover();
    }

    /*
     * Returns a copy of the area of the whole preview, including bits clipped by the view
     */
    public RectF getTextureArea() {

        if (mTextureView == null) {
            return new RectF();
        }
        Matrix matrix = new Matrix();
        RectF area = new RectF(0, 0, mPreviewWidth, mPreviewHeight);
        mTextureView.getTransform(matrix).mapRect(area);
        return area;
    }

    public Bitmap getPreviewBitmap(int downsample, int orientation) {
        RectF textureArea = getTextureArea();
        int width = (int) textureArea.width() / downsample;
        int height = (int) textureArea.height() / downsample;
        Bitmap preview = mTextureView.getBitmap(width, height);
        
        Matrix transform = mTextureView.getTransform(null);
        transform.postRotate(orientation);
        return Bitmap.createBitmap(preview, 0, 0, width, height, transform, true);
    }

    public void updatePreviewAspectRatio(float ratio) {
        if (mAspectRatio != ratio) {
            if (ratio < 1) {
                mAspectRatio = 1 / ratio;
            } else {
                mAspectRatio = ratio;
            }

            RectF dstRect = new RectF(0, 0, mPreviewWidth, (int) (mPreviewWidth * ratio));
            Log.e(TAG, "updatePreviewAspectRatio ratio:" + mAspectRatio + ":dstRect:" + dstRect.toString());

            Matrix transform = new Matrix();
            transform.setRectToRect(new RectF(0, 0, mPreviewWidth, mPreviewHeight), dstRect, Matrix.ScaleToFit.FILL);
            mTextureView.setTransform(transform);
            // Notify preview changed
            mController.onPreviewRectChanged(CameraUtil.rectFToRect(dstRect), mAspectRatio);
        }
    }

}
