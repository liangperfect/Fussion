package com.westalgo.factorycamera.module;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.westalgo.factorycamera.debug.Log;

public class DualTextureViewHelper implements
    TextureView.SurfaceTextureListener {
    private static final Log.Tag TAG = new Log.Tag("DualTextureViewHelper");
    public static final float MATCH_SCREEN = 0f;
    private TextureView mTextureView;
    private boolean mIsMainCameraView;
    private DualTextureViewListener mSurfaceTextureListener;


    public DualTextureViewHelper(TextureView preview, boolean isMainCameraView) {
        mTextureView = preview;
        mIsMainCameraView = isMainCameraView;
        mTextureView.setSurfaceTextureListener(this);
    }

    public void setSurfaceTextureListener(DualTextureViewListener listener) {
        mSurfaceTextureListener = listener;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureAvailable(surface, width,
                    height, mIsMainCameraView);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {

        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width,
                    height, mIsMainCameraView);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureDestroyed(surface,
                    mIsMainCameraView);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureUpdated(surface,
                    mIsMainCameraView);
        }
    }

}