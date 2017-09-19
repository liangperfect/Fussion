package com.westalgo.factorycamera.module;

import android.graphics.Rect;

public interface PhotoController {

    public static final int INIT = -1;
    public static final int PREVIEW_STOPPED = 0;
    public static final int IDLE = 1;  // preview is active
    // Focus is in progress. The exact focus state is in Focus.java.
    public static final int FOCUSING = 2;
    public static final int SNAPSHOT_IN_PROGRESS = 3;
    // Switching between cameras.
    public static final int SWITCHING_CAMERA = 4;

    public void cancelAutoFocus();

    public void startPreview(boolean isMain);

    public void stopPreview(boolean isMain);

    public void onSingleTapUp( int x, int y);

    /**
     * This is the callback when the UI or buffer holder for camera preview,
     * such as {@link android.graphics.SurfaceTexture}, is ready to be used.
     * The controller can start the camera preview after or in this callback.
     */
    public void onPreviewUIReady(boolean isMain);


    /**
     * This is the callback when the UI or buffer holder for camera preview,
     * such as {@link android.graphics.SurfaceTexture}, is being destroyed.
     * The controller should try to stop the preview in this callback.
     */
    public void onPreviewUIDestroyed(boolean isMain);


    public void onPreviewRectChanged(Rect previewRect, float ratio);

}
