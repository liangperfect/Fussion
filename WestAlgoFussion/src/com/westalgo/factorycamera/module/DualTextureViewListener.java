package com.westalgo.factorycamera.module;

import android.graphics.SurfaceTexture;
import android.view.GestureDetector;

public interface DualTextureViewListener {

    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height, boolean isMain);

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height, boolean isMain);

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface, boolean isMain);

    public void onSurfaceTextureUpdated(SurfaceTexture surface, boolean isMain);

}
