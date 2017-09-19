
package com.westalgo.factorycamera.module;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import com.westalgo.factorycamera.debug.Log;

public class TextureViewHelper implements TextureView.SurfaceTextureListener {
    private static final Log.Tag TAG = new Log.Tag("TextureViewHelper");
    public static final float MATCH_SCREEN = 0f;
    private TextureView mTextureView;
    private TextureViewListener mSurfaceTextureListener;

    public TextureViewHelper(TextureView preview) {
        mTextureView = preview;
        mTextureView.setSurfaceTextureListener(this);
    }

    public void setSurfaceTextureListener(TextureViewListener listener) {
        mSurfaceTextureListener = listener;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }

}
