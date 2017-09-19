package com.westalgo.factorycamera.module;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.util.DisplayMetrics;
import android.content.SharedPreferences;
import android.content.Context;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.FocusOverlayManager.FocusUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.ui.PreviewStatusListener;
import com.westalgo.factorycamera.util.CameraUtil;
import android.content.res.Resources;

public class DualBaseCameraUI implements DualTextureViewListener, PreviewStatusListener {

    private static final Log.Tag TAG = new Log.Tag("DualBaseCameraUI");
    private CameraActivity mActivity;
    private PhotoController mController;
    private FrameLayout mLayout;
    // Main textureView
    private DualTextureViewHelper mMainTextureViewHelper;
    private TextureView mMainTextureView = null;
    private SurfaceTexture mMainSurfaceTexture = null;
    // Sub TexttureView
    private DualTextureViewHelper mSubTextureViewHelper;
    private TextureView mSubTextureView = null;
    private SurfaceTexture mSubSurfaceTexture = null;
    //Just update main TextureView layout
    private float mAspectRatio = DualTextureViewHelper.MATCH_SCREEN;
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
            if (mPreviewWidth != width || mPreviewHeight != height) {
                // Eirot add for landscape start
                if (mActivity.getCurrentModuleIndex() == 1) {
                    updateTextureView();
                } else {
                    mPreviewWidth = width;
                    mPreviewHeight = height;
                }
                // Eirot add for landscape end
            }
        }
    };

    public DualBaseCameraUI(CameraActivity activity, PhotoController controller, FrameLayout parent) {
        mActivity = activity;
        mController = controller;
        mLayout = (FrameLayout) parent.findViewById(R.id.texture_view_root);
        mLayout.removeAllViews();

        mFocusUI = (FocusUI) parent.findViewById(R.id.focus_overlay);

        mActivity.getLayoutInflater().inflate(R.layout.dual_camera_module, (ViewGroup) mLayout, true);
        mMainTextureView = (TextureView) mLayout.findViewById(R.id.main_preview_content);
        mMainTextureViewHelper = new DualTextureViewHelper(mMainTextureView, true);
        mMainTextureViewHelper.setSurfaceTextureListener(this);

        mSubTextureView = (TextureView) mLayout.findViewById(R.id.sub_preview_content);
        mSubTextureViewHelper = new DualTextureViewHelper(mSubTextureView, false);
        mSubTextureViewHelper.setSurfaceTextureListener(this);

        if (activity.getCurrentModuleIndex() == 1) {
            updateTextureView();
        }
        mMainTextureView.addOnLayoutChangeListener(mLayoutListener);
    }

    public SurfaceTexture getMainSurfaceTexture() {
        return mMainSurfaceTexture;
    }

    public SurfaceTexture getSubSurfaceTexture() {
        return mSubSurfaceTexture;
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
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height, boolean isMain) {
        Log.v(TAG, "onSurfaceTextureAvailable isMain:"+isMain + " width " + width + " height " + height);
        // TODO Auto-generated method stub
        if (isMain) {
            mMainSurfaceTexture = surface;
        } else {
            mSubSurfaceTexture = surface;
        }

        mController.onPreviewUIReady(isMain);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height, boolean isMain) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface, boolean isMain) {
        Log.v(TAG, "onPreviewUIDestroyed isMain:"+isMain);
        mController.onPreviewUIDestroyed(isMain);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface, boolean isMain) {
        // TODO Auto-generated method stub
        if (isMain) {
            mMainSurfaceTexture = surface;
            mActivity.hideModeCover();
        } else {
            mSubSurfaceTexture = surface;
        }

    }

    /*
    * Returns a copy of the area of the whole preview, including bits clipped
    * by the view
    */
    public RectF getTextureArea() {

        if (mMainTextureView == null) {
            return new RectF();
        }
        Matrix matrix = new Matrix();
        RectF area = new RectF(0, 0, mPreviewWidth, mPreviewHeight);
        mMainTextureView.getTransform(matrix).mapRect(area);
        return area;
    }

    public Bitmap getPreviewBitmap(int downsample, int orientation) {
        RectF textureArea = getTextureArea();
        int width = (int) textureArea.width() / downsample;
        int height = (int) textureArea.height() / downsample;
        Bitmap preview = mMainTextureView.getBitmap(width, height);

        Matrix transform = mMainTextureView.getTransform(null);
        transform.postRotate(orientation);
        return Bitmap.createBitmap(preview, 0, 0, width, height, transform, true);
    }

    public void updatePreviewAspectRatio(float ratio) {
        if (mAspectRatio != ratio) {
            if(ratio < 1) {
                mAspectRatio = 1 / ratio;
            } else {
                mAspectRatio = ratio;
            }

            RectF dstRect = new RectF(0, 0, mPreviewWidth, (int) (mPreviewWidth * ratio));
            Log.e(TAG,"--> updatePreviewAspectRatio ratio:" + mAspectRatio + ":dstRect:" + dstRect.toString());

            Matrix transform = new Matrix();
            transform.setRectToRect(new RectF(0, 0, mPreviewWidth, mPreviewHeight), dstRect, Matrix.ScaleToFit.FILL);
            mMainTextureView.setTransform(transform);
            mSubTextureView.setTransform(transform);
            // Notify preview changed
            mController.onPreviewRectChanged(CameraUtil.rectFToRect(dstRect), mAspectRatio);
//            mController.onPreviewRectChanged(new Rect(0,0,mPreviewWidth,(int)(mPreviewWidth * PREVIEW_ASPECT)), mAspectRatio);
        }
    }

    /**
     * update MainTextureView and SubTextureView Layout params only for Landscape
     */
    public void updateTextureView() {
        boolean isLandscape = CameraUtil.isLandscape(mActivity);
        if (isLandscape) {
            Resources resources = mActivity.getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            int widthPixels = dm.widthPixels;
            int heightPixels = dm.heightPixels;
            int mLandWidth = widthPixels > heightPixels ? widthPixels : heightPixels;

            int landscapePreviewWidth = mLandWidth / 2;
            int landscapePreviewHeight;
            SettingsManager settingsManager = mActivity.getSettingsManager();
            boolean isAspect16to9 = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                    Keys.KEY_USER_SELECTED_ASPECT_RATIO_BACK);

            landscapePreviewHeight = (int)(landscapePreviewWidth  * (3.0f / 4.0f));
            if (isAspect16to9) {
                landscapePreviewHeight = (int)(landscapePreviewWidth * (9.0f / 16.0f));
            }

            // MainTextureView
            FrameLayout.LayoutParams mParamsMain = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            mParamsMain.width = landscapePreviewWidth;
            mParamsMain.height = landscapePreviewHeight;
            mMainTextureView.setLayoutParams(mParamsMain);

            // SubTextureView
            FrameLayout.LayoutParams mParamsSub = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            mParamsSub.width = landscapePreviewWidth;
            mParamsSub.height = landscapePreviewHeight;
            mParamsSub.leftMargin = landscapePreviewWidth;
            mSubTextureView.setLayoutParams(mParamsSub);

            mPreviewWidth = landscapePreviewHeight;
            mPreviewHeight = landscapePreviewWidth;

            SharedPreferences preferences = mActivity.getSharedPreferences("DualCamera", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("landscapePreviewWidth", landscapePreviewWidth);
            editor.putInt("landscapePreviewHeight", landscapePreviewHeight);
            editor.putBoolean("isAspect16to9", isAspect16to9);
            editor.commit();
        }
    }
}
