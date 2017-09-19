package com.westalgo.factorycamera;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.util.CameraUtil;


public class FocusOverlayManager {

    private static final Log.Tag TAG = new Log.Tag("FocusOverlayMgr");

    private int mState = STATE_IDLE;
    private static final int STATE_IDLE = 0; // Focus is not active.
    private static final int STATE_FOCUSING = 1; // Focus is in progress.
    // Focus is in progress and the camera should take a picture after focus finishes.
    private static final int STATE_FOCUSING_SNAP_ON_FINISH = 2;
    private static final int STATE_SUCCESS = 3; // Focus finishes and succeeds.
    private static final int STATE_FAIL = 4; // Focus finishes and fails.

    private static final int RESET_TOUCH_FOCUS = 0;
    private static final int RESET_TOUCH_FOCUS_DELAY_MILLIS = CameraUtil.FOCUS_HOLD_MILLIS;


    private final SettingsManager mSettingsManager;
    private final Handler mHandler;
    Listener mListener;
    private boolean mPreviousMoving;
    private final FocusUI mUI;
    private final Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private Matrix mMatrix;
    private boolean mMirror; // true if the camera is front-facing.
    private int mDisplayOrientation;
    private List<Area> mFocusArea; // focus area in driver format
    private List<Area> mMeteringArea; // metering area in driver format
    private List<Area> mSubFocusCenterArea;
    private List<Area> mSubMeteringCenterArea;
    private boolean mInitialized;
    private String mFocusMode;
//    private boolean mFocusAreaSupported;
//    private boolean mMeteringAreaSupported;

    public interface FocusUI {
        public void clearFocus();

        public void setFocusPosition(int x, int y);

        public void setFocusPosition(int x, int y, int afRegineSize, int aeRegineSize);

        public void onFocusStarted();

        public void onFocusSucceeded();

        public void onFocusFailed();
    }

    public interface Listener {
        public void autoFocus();

        public void cancelAutoFocus();

        public boolean capture();

        public void setFocusParameters();
    }

    private static class MainHandler extends Handler {
        final WeakReference<FocusOverlayManager> mManager;
        public MainHandler(FocusOverlayManager manager, Looper looper) {
            super(looper);
            mManager = new WeakReference<FocusOverlayManager>(manager);
        }

        @Override
        public void handleMessage(Message msg) {
            FocusOverlayManager manager = mManager.get();
            if (manager == null) {
                return;
            }

            switch (msg.what) {
            case RESET_TOUCH_FOCUS: {
                manager.cancelAutoFocus();
                break;
            }
            }
        }
    }

    public FocusOverlayManager(CameraActivity activity,Parameters parameters, Listener listener,
                               boolean mirror, Looper looper, FocusUI ui) {
        mSettingsManager = activity.getSettingsManager();
        mHandler = new MainHandler(this, looper);
        mMatrix = new Matrix();
        mListener = listener;
        setMirror(mirror);
        mUI = ui;

    }

    /** Returns width of auto focus region in pixels. */
    private int getAFRegionEdge() {
        return (int) (Math.min(mPreviewRect.width(), mPreviewRect.height()) * CameraUtil.AF_REGION_BOX);
    }

    /** Returns width of metering region in pixels. */
    private int getAERegionEdge() {
        return (int) (Math.min(mPreviewRect.width(), mPreviewRect.height()) * CameraUtil.AE_REGION_BOX);
    }

    private void initializeFocusAreas(int x, int y) {
        if (mFocusArea == null) {
            mFocusArea = new ArrayList<Area>();
            mFocusArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        calculateTapArea(x, y, getAFRegionEdge(), mFocusArea.get(0).rect);
    }

    private void initializeMeteringAreas(int x, int y) {
        if (mMeteringArea == null) {
            mMeteringArea = new ArrayList<Area>();
            mMeteringArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        calculateTapArea(x, y, getAERegionEdge(), mMeteringArea.get(0).rect);
    }

    private void initializeFocusAreasForSub(int x, int y) {
        if (mSubFocusCenterArea == null) {
            mSubFocusCenterArea = new ArrayList<Area>();
            mSubFocusCenterArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        calculateTapArea(x, y, getAFRegionEdge(), mSubFocusCenterArea.get(0).rect);
    }

    public void removeMessages() {
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    private void initializeMeteringAreasForSub(int x, int y) {
        if (mSubMeteringCenterArea == null) {
            mSubMeteringCenterArea = new ArrayList<Area>();
            mSubMeteringCenterArea.add(new Area(new Rect(), 1));
        }

        // Convert the coordinates to driver format.
        calculateTapArea(x, y, getAERegionEdge(), mSubMeteringCenterArea.get(0).rect);
    }

    public List<Area> getFocusAreas() {
        return mFocusArea;
    }

    public List<Area> getMeteringAreas() {
        return mMeteringArea;
    }

    public List<Area> getFocusAreasForSub() {
        return mSubFocusCenterArea;
    }

    public List<Area> getMeteringAreasForSub() {
        return mSubMeteringCenterArea;
    }

    private void calculateTapArea(int x, int y, int size, Rect rect) {
        int left = CameraUtil.clamp(x - size / 2, mPreviewRect.left, mPreviewRect.right - size);
        int top = CameraUtil.clamp(y - size / 2, mPreviewRect.top, mPreviewRect.bottom - size);

        RectF rectF = new RectF(left, top, left + size, top + size);
        mMatrix.mapRect(rectF);
        CameraUtil.rectFToRect(rectF, rect);
    }

    public void setMirror(boolean mirror) {
        mMirror = mirror;
        setMatrix();
    }

    public void setDisplayOrientation(int orientation){
        if(orientation != mDisplayOrientation){
            mDisplayOrientation = orientation;
            setMatrix();
        }
    }

    public void onPreviewAreaChanged(Rect previewArea) {
        Log.e(TAG,"onPreviewAreaChanged");
        if (mPreviewRect != null) {
            mPreviewRect.set(previewArea);
            setMatrix();
            initAreasForSub();
        }
    }

    public void initAreasForSub() {
        if (mPreviewRect.width() != 0 && mPreviewRect.height() != 0) {
            initializeFocusAreasForSub(mPreviewRect.centerX(), mPreviewRect.centerY());
            initializeMeteringAreasForSub(mPreviewRect.centerX(), mPreviewRect.centerY());
        }
    }
    /**
     * Returns a copy of mPreviewRect so that outside class cannot modify
     * preview rect except deliberately doing so through the setter.
     */
    public Rect getPreviewRect() {
        return new Rect(mPreviewRect);
    }

    private void setMatrix() {
        if (mPreviewRect.width() != 0 && mPreviewRect.height() != 0) {
            Matrix matrix = new Matrix();
            CameraUtil.prepareMatrix(matrix, mMirror, mDisplayOrientation, getPreviewRect());
            // In face detection, the matrix converts the driver coordinates to
            // UI coordinates. In tap focus, the inverted matrix converts the UI
            // coordinates to driver coordinates.
            matrix.invert(mMatrix);
            mInitialized = true;
        }
    }

    public void onSingleTapUp(int x, int y) {
        if (!mInitialized || mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            return;
        }

        // Let users be able to cancel previous touch focus.
        if ((mFocusArea != null) && (mState == STATE_FOCUSING ||
                                     mState == STATE_SUCCESS || mState == STATE_FAIL)) {
            cancelAutoFocus();
        }
        if (mPreviewRect.width() == 0 || mPreviewRect.height() == 0) {
            return;
        }
        initializeFocusAreas(x, y);
        initializeMeteringAreas(x, y);

        // Use margin to set the focus indicator to the touched area.
        mUI.setFocusPosition(x, y);

        // Set the focus area and metering area.

        mListener.setFocusParameters();
        autoFocus();
    }

    /**
     * Triggers the autofocus and set the state to indicate the focus is in
     * progress.
     */
    private void autoFocus() {
        autoFocus(STATE_FOCUSING);
    }

    /**
     * Triggers the autofocus and sets the specified state.
     *
     * @param focusingState The state to use when focus is in progress.
     */
    private void autoFocus(int focusingState) {
        mListener.autoFocus();
        mState = focusingState;
        updateFocusUI();
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    public void updateFocusUI() {
        if (!mInitialized) {
            return;
        }
        if (mState == STATE_IDLE) {
            if (mFocusArea == null) {
                mUI.clearFocus();
            } else {
                // Users touch on the preview and the indicator represents the
                // metering area. Either focus area is not supported or
                // autoFocus call is not required.
                mUI.onFocusStarted();
            }
        } else if (mState == STATE_FOCUSING || mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            mUI.onFocusStarted();
        } else {
            if (mState == STATE_SUCCESS) {
                mUI.onFocusSucceeded();
            } else if (mState == STATE_FAIL) {
                mUI.onFocusFailed();
            }
        }
    }

    public String getFocusMode() {
        if(mFocusArea != null) {
            mFocusMode = Camera.Parameters.FOCUS_MODE_AUTO;
        } else {
            mFocusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
        }
        return mFocusMode;
    }

    public void onAutoFocusMoving(boolean moving, boolean focused) {
        Log.v(TAG,"onAutoFocusMoving: moving:"+moving + ":state:" + mState);

        if (!mInitialized) {
            return;
        }

        // Ignore if we have requested autofocus. This method only handles continuous autofocus.
        /*
        if (mState != STATE_IDLE) {
            return;
        }*/

        if(mUI ==  null) {
            Log.v(TAG, "mUI is null ");
        }
        // animate on false->true trasition only
        if (moving && !mPreviousMoving) {
            // Auto focus at the center of the preview.
            mUI.setFocusPosition(mPreviewRect.centerX(), mPreviewRect.centerY());
            mUI.onFocusStarted();
        } else if (!moving) {
            if(focused) {
                mUI.onFocusSucceeded();
            } else {
                mUI.onFocusFailed();
            }
        }
        mPreviousMoving = moving;

        if (mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
            capture();
        } else if (mState == STATE_FOCUSING) {
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
        }
    }

    public void onAutoFocusMoving(boolean moving) {
        if (!mInitialized) return;

        // Ignore if we have requested autofocus. This method only handles
        // continuous autofocus.
        if (mState != STATE_IDLE) return;

        // animate on false->true trasition only b/8219520
        if (moving && !mPreviousMoving) {
            mUI.setFocusPosition(mPreviewRect.centerX(), mPreviewRect.centerY());
            mUI.onFocusStarted();
        } else if (!moving) {
            mUI.onFocusSucceeded();
        }
        mPreviousMoving = moving;
    }

    public void onAutoFocus(boolean focused) {
        Log.v(TAG,"onAutoFocus  focused:"+  focused);
        if (mState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
            capture();
        } else if (mState == STATE_FOCUSING) {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused) {
                mState = STATE_SUCCESS;
            } else {
                mState = STATE_FAIL;
            }
            updateFocusUI();
            // If this is triggered by touch focus, cancel focus after a
            // while.
            if (mFocusArea != null) {
                mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY_MILLIS);
            }
        }
    }

    private void cancelAutoFocus() {
        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        resetTouchFocus();
        mListener.cancelAutoFocus();
        mPreviousMoving = false;
        mState = STATE_IDLE;
        updateFocusUI();
        mHandler.removeMessages(RESET_TOUCH_FOCUS);
    }

    public void resetTouchFocus() {
        Log.v(TAG,"resetTouchfocus");
        if (!mInitialized) {
            return;
        }

        // Put focus indicator to the center. clear reset position
        mUI.clearFocus();
        // Initialize mFocusArea.
        mFocusArea = null;
        mMeteringArea = null;
        // This will cause current module to call getFocusAreas() and
        // getMeteringAreas() and send updated regions to camera.
        mListener.setFocusParameters();
    }

    public boolean isFocusCompleted() {
        return mState == STATE_SUCCESS || mState == STATE_FAIL;
    }

    public boolean isFocusingSnapOnFinish() {
        return mState == STATE_FOCUSING_SNAP_ON_FINISH;
    }

    public void focusAndCapture(boolean needFocus) {
        Log.v(TAG,"focusAndCapture");
        if (!mInitialized) {
            return;
        }

        if(mState == STATE_SUCCESS || mState == STATE_FAIL) {
            // Focus is done already.
            capture();
        } else if (mState == STATE_FOCUSING) {
            // Still focusing and will not trigger snap upon finish.
            mState = STATE_FOCUSING_SNAP_ON_FINISH;
        } else if (mState == STATE_IDLE) {
            if (needFocus) {
                autoFocusAndCapture();
            } else {
                capture();
            }
        }
    }

    /**
     * Triggers the autofocus and set the state to which a capture will happen
     * in the following autofocus callback.
     */
    private void autoFocusAndCapture() {
        autoFocus(STATE_FOCUSING_SNAP_ON_FINISH);
    }

    private void capture() {
        Log.d(TAG,"[TIME_TAG]APP:capture --> capture start!");
        if (mListener.capture()) {
            mState = STATE_IDLE;
            mHandler.removeMessages(RESET_TOUCH_FOCUS);
        }
    }


    public void onPreviewStopped() {
        // If auto focus was in progress, it would have been stopped.
        mState = STATE_IDLE;
        updateFocusUI();
    }
}
