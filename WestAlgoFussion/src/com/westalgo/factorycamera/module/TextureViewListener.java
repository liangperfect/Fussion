
package com.westalgo.factorycamera.module;

import android.graphics.SurfaceTexture;
import android.view.GestureDetector;

public interface TextureViewListener {

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height);

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height);

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface);

    public void onSurfaceTextureUpdated(SurfaceTexture surface);

}
