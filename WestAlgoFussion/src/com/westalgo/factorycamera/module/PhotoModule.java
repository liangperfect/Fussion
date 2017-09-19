package com.westalgo.factorycamera.module;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.FrameLayout;

import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.Exif;
import com.westalgo.factorycamera.FocusOverlayManager;
import com.westalgo.factorycamera.MediaSaveService;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.app.CameraAppUI.BottomBarUISpec;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.exif.ExifInterface;
import com.westalgo.factorycamera.manager.CameraHolder;
import com.westalgo.factorycamera.manager.CameraManager.CameraAFCallback;
import com.westalgo.factorycamera.manager.CameraManager.CameraAFMoveCallback;
import com.westalgo.factorycamera.manager.CameraManager.CameraPictureCallback;
import com.westalgo.factorycamera.manager.CameraManager.CameraProxy;
import com.westalgo.factorycamera.manager.CameraManager.CameraShutterCallback;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingUtil;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.util.CameraUtil.NamedImages;
import com.westalgo.factorycamera.util.CameraUtil.NamedImages.NamedEntity;

import java.util.List;

public class PhotoModule implements PhotoController, ModuleController,
        SettingsManager.OnSettingChangedListener, FocusOverlayManager.Listener {
    private static final Log.Tag TAG = new Log.Tag("PhotoModule");
    public static final String PHOTO_MODULE_STRING_ID = "PhotoModule";
    private static final float PREVIEW_ASPECT = (float) 4 / 3;
    private static final int SETUP_PREVIEW = 1;
    private static final int FIRST_TIME_INIT = 2;
    private static final int CLEAR_SCREEN_DELAY = 3;
    private static final int SET_CAMERA_PARAMETERS_WHEN_IDLE = 4;
    private static final int SHOW_TAP_TO_FOCUS_TOAST = 5;
    private static final int SWITCH_CAMERA = 6;
    private static final int SWITCH_CAMERA_START_ANIMATION = 7;
    private static final int CAMERA_OPEN_DONE = 8;
    private static final int OPEN_CAMERA_FAIL = 9;
    private static final int CAMERA_DISABLED = 10;
    private static final int ON_PREVIEW_STARTED = 15;
    private static final int PREVIEW_SIZE_CHANGED = 16;

    // The subset of parameters we need to update in setCameraParameters().
    private static final int UPDATE_PARAM_PICTURESIZE = 1;
    private static final int UPDATE_PARAM_PREFERENCE = 2;
    private static final int UPDATE_PARAM_ALL = -1;
    private int mOrientation = 0;

    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final AutoFocusMovingCallback mAutoFocusMoveCallback = new AutoFocusMovingCallback();

    private CameraActivity mActivity;
    private ContentResolver mContentResolver;

    private Handler mHandler;
    private CameraProxy mMainCameraDevice;
    private Parameters mMainParameters;
    private int mMainCameraId = CameraHolder.SINGLE_CAMERA_ID;

    private boolean mShouldResizeTo16x9 = false;
    private int mainJpegRotation;

    private boolean mPaused = false;
    private FrameLayout mRootView;

    private PhotoUI mUI;
    private OpenCameraThread mOpenCameraThread = null;

    private FocusOverlayManager mFocusManager;
    private boolean mMirror;
    private boolean mFocused = false;

    protected int mCameraState = PREVIEW_STOPPED;

    // when autofocus is focusing and a picture will be taken when
    // focus callback arrives.
    private boolean mSnapshotOnIdle = false;

    public NamedImages mNamedImages;
    private NamedEntity mNamedEntity;
    public long mCaptureStartTime;
//    private PhotoModeCallbackListener mPhotoModeCallbackListener;
    // When setting changed
    private boolean mLsGlobalSettingChanged = false;

    public PhotoModule() {

    }

    private class MainHandler extends Handler {
        private MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
            case CAMERA_OPEN_DONE:
                onCameraOpened();
                break;
            case PREVIEW_SIZE_CHANGED:
                int width = msg.arg1;
                int height = msg.arg2;
                float ratio = (float) width / (float) height;
                mUI.updatePreviewAspectRatio(ratio);
                break;
            case ON_PREVIEW_STARTED:
                onPreviewStarted();
                break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public void init(CameraActivity activity, View parent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "init");
        mActivity = activity;
        mContentResolver = mActivity.getContentResolver();
        mRootView = (FrameLayout) parent;
        mHandler = new MainHandler();
        mUI = new PhotoUI(activity, this, mRootView);

        mActivity.getCameraAppUI().setPreviewStatusListener(mUI);

        if (mOpenCameraThread == null) {
            mOpenCameraThread = new OpenCameraThread();
            mOpenCameraThread.start();
        }
    }

    /**
     * Main camera do normal focus. If sub camera support AF, main camera do
     * focus --> return main camera focus result -> do sub camera focus -->
     * return sub camera focus result
     *
     * If sub camera not support AF, main camera do focus, sub camera do fix
     * focus
     */
    public void initializeFocusManager() {
        CameraInfo info = CameraHolder.instance().getCameraInfo()[mMainCameraId];
        mMirror = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        synchronized (this) {
            if (mFocusManager == null) {
                mFocusManager = new FocusOverlayManager(mActivity,
                                                        mMainParameters, this, mMirror,
                                                        mActivity.getMainLooper(), mUI.getFocusUI());
            } else {
                mFocusManager.removeMessages();
            }
        }
    }

    private class OpenCameraThread extends Thread {
        @Override
        public void run() {
            // initializeFocusManager();
            openMainCamera();
            // mHandler.sendEmptyMessageDelayed(CAMERA_OPEN_DONE, 100);
            startPreview(true);
        }
    }

    private void openMainCamera() {
        // We need to check whether the activity is paused before long
        // operations to ensure that onPause() can be done ASAP.
        if (mPaused) {
            return;
        }
        Log.d(TAG, "openMainCamera");
        mMainCameraId = CameraHolder.SINGLE_CAMERA_ID;
        mMainCameraDevice = CameraHolder.instance().openMainCamera(mHandler,
                            mMainCameraId, mActivity.getCameraOpenErrorCallback());
        if (mMainCameraDevice == null) {
            Log.e(TAG, "Failed to open camera:" + mMainCameraId);
            mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            return;
        }
        mMainParameters = mMainCameraDevice.getParameters();

        initializeFocusManager();
        mHandler.sendEmptyMessage(CAMERA_OPEN_DONE);
    }

    public void resume() {
        mPaused = false;
        if (mOpenCameraThread == null) {
            mOpenCameraThread = new OpenCameraThread();
            mOpenCameraThread.start();
        }
        mNamedImages = new NamedImages();
    }

    public void pause() {
        mPaused = true;
        mNamedImages = null;
        try {
            if (mOpenCameraThread != null) {
                mOpenCameraThread.join();
            }
        } catch (InterruptedException ex) {
        }
        mOpenCameraThread = null;

        if (mFocusManager != null) {
            mFocusManager.removeMessages();
        }

        cancelAutoFocus();
        stopPreview(mMainCameraDevice);

        CameraHolder.instance().releaseMain();

        mMainCameraDevice = null;
    }

    public void destroy() {
        // TODO Auto-generated method stub
        SettingUtil.forceUpdateISO = true;
    }

    public void onPreviewVisibilityChanged(int visibility) {
        // TODO Auto-generated method stub

    }

    public void onOrientationChanged(int orientation) {
        // TODO Auto-generated method stub
        mOrientation = orientation;
    }

    /**************************** Hardware Support Api ******************************/
    // Check weather supported FLASH MODE.
    private boolean isFlashSupported() {
        return (CameraUtil.isSupported(Parameters.FLASH_MODE_AUTO,
                                       mMainParameters.getSupportedFlashModes()) || CameraUtil
                .isSupported(Parameters.FLASH_MODE_ON,
                             mMainParameters.getSupportedFlashModes()));
    }

    /**************************** Preview Api ******************************/
    private void onPreviewStarted() {
        setCameraState(IDLE);
        mActivity.setShutterEnabled(true);
    }

    private void onCameraOpened() {
        if (mPaused) {
            return;
        }
        if (!mActivity.getSettingsManager().isSet(SettingsManager.SCOPE_GLOBAL,
                Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA)) {
            mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL,
                                               Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA,
                                               isFlashSupported());

        }
        mActivity.getCameraAppUI().onChangeCamera();
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.addListener(this);
        Log.v(TAG, "onCameraOpened");
    }


    /** Only called by UI thread. */
    public void setupPreview(boolean isMain) {
        Log.i(TAG, "setupPreview");
        mFocusManager.resetTouchFocus();
        startPreview(true);
    }

    /**
     * This can run on a background thread, so don't do UI updates here. Post
     * any view updates to MainHandler or do it on onPreviewStarted() .
     */
    @Override
    public void startPreview(boolean isMain) {
        Log.v(TAG, "startPreview");
        if (mPaused || (mMainCameraDevice == null) || (mMainParameters == null)) {
            return;
        }

        SurfaceTexture st = null;
        if (mUI != null) {
            st = mUI.getMainSurfaceTexture();
        }
        // Surfacetexture could be null here, but its still valid and safe
        // to set null
        // surface before startpreview. This will help in basic preview
        // setup and
        // surface creation in parallel. Once valid surface is ready in
        // onPreviewUIReady()
        // we set the surface to camera to actually start preview.
        mMainCameraDevice.setPreviewTexture(st);
        // mMainCameraDevice.setErrorCallback(mErrorCallback);
        setCameraParameters(mMainCameraDevice, mMainParameters, UPDATE_PARAM_ALL);
        setDisplayOrientation(mMainCameraDevice, mMainCameraId);
        mMainCameraDevice.startPreview();
        mHandler.sendEmptyMessage(ON_PREVIEW_STARTED);
    }

    public void stopPreview(CameraProxy cameraDevice) {
        if (cameraDevice != null) {
            Log.v(TAG, "stopPreview");
            cameraDevice.stopPreview();
        }
        setCameraState(PREVIEW_STOPPED);
        if (mFocusManager != null) {
            mFocusManager.onPreviewStopped();
        }
    }

    private void setDisplayOrientation(CameraProxy cameraDevice, int cameraId) {
        int mDisplayOrientation = CameraUtil.getCameraDisplayOrientation(
                                      mActivity.getAndroidContext(), cameraId);
        if (mUI != null) {
            mUI.setDisplayOrientation(mDisplayOrientation);
        }
        // Change the camera display orientation
        if (cameraDevice != null) {
            cameraDevice.setDisplayOrientation(mDisplayOrientation);
        }
    }

    @Override
    public void stopPreview(boolean isMain) {
        if (mMainCameraDevice != null) {
            mMainCameraDevice.stopPreview();
        }
    }

    @Override
    public void onPreviewUIReady(boolean isMain) {
        if (mPaused || mMainCameraDevice == null) {
            return;
        }
        SurfaceTexture st = mUI.getMainSurfaceTexture();
        if (st == null) {
            Log.e(TAG, "startPreview: main surfaceTexture is not ready.");
            return;
        }
        mMainCameraDevice.setPreviewTexture(st);
    }

    @Override
    public void onPreviewUIDestroyed(boolean isMain) {
        Log.d(TAG, "onPreviewUIDestroyed, stopPreview");
        stopPreview(isMain);
    }

    @Override
    public void onPreviewRectChanged(Rect previewRect, float ratio) {
        mFocusManager.onPreviewAreaChanged(previewRect);
        if (ratio != PREVIEW_ASPECT) {
            mActivity.onPreviewAreaChanged(new Rect(0, 0, previewRect.width(),
                                                    (int) (previewRect.width() * PREVIEW_ASPECT)), ratio);
        } else {
            mActivity.onPreviewAreaChanged(previewRect, ratio);
        }
    }

    private void setCameraState(int state) {
        mCameraState = state;
    }

    public Bitmap getPreviewBitmap(int downsample) {
        return mUI == null ? null : mUI.getPreviewBitmap(downsample, mOrientation);
    }

    /**************************** Update Parameters Api ******************************/

    public void setGlobalSettingChanged(boolean isChanged) {
        mLsGlobalSettingChanged = isChanged;
    }

    // We separate the parameters into several subsets, so we can update only
    // the subsets actually need updating. The PREFERENCE set needs extra
    // locking because the preference can be changed from GLThread as well.
    private void setCameraParameters(CameraProxy cameraDevice, Parameters parameters, int updateSet) {
        if (cameraDevice == null || parameters == null) {
            return;
        }
        synchronized (cameraDevice) {
            if ((updateSet & UPDATE_PARAM_PICTURESIZE) != 0) {
                updateParametersPictureSize(cameraDevice.getCameraId(), parameters);
                updatePrametersSettings(cameraDevice, parameters);
            }
            if ((updateSet & UPDATE_PARAM_PREFERENCE) != 0) {
                updateCameraParametersPreference(cameraDevice, parameters);
            }
            cameraDevice.enableShutterSound(true);
            cameraDevice.setParameters(parameters);
        }
    }

    private void updatePrametersSettings(CameraProxy cameraDevice, Parameters parameters) {
        parameters.set(Keys.KEY_AUTOEXPOSURE, "center-weighted");
        parameters.setAntibanding(Parameters.ANTIBANDING_50HZ);
        parameters.setRotation(0);
        parameters.set(Keys.KEY_DUALCAM_MODE, "normal");
    }

    private void updateCameraParametersPreference(CameraProxy cameraDevice, Parameters parameters) {
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setFocusAreas(mFocusManager.getFocusAreas());
        parameters.setMeteringAreas(mFocusManager.getMeteringAreas());
        parameters.setFocusMode(mFocusManager.getFocusMode());
        // TODO Query whether hardware support it
        updateAutoFocusMoveCallback(cameraDevice, parameters);

        // Set Exposure Compensation
        updateParametersExposureCompensation(cameraDevice, parameters);
        // Set Flash Mode
        updateParametersFlashMode(cameraDevice, parameters);
        // Set ISO
        updateParametersISO(cameraDevice, parameters);
    }

    private void updateAutoFocusMoveCallback(CameraProxy cameraDevice, Parameters parameters) {
        if (cameraDevice == null || parameters == null) {
            return;
        }
        Log.d(TAG, "updateAutoFocusMoveCallback, focus mode: "+parameters.getFocusMode());
        if (parameters.getFocusMode() == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
            cameraDevice.setAutoFocusMoveCallback(mHandler, mAutoFocusMoveCallback);
        } else {
            cameraDevice.setAutoFocusMoveCallback(null, null);
        }
    }

    private void updateParametersPictureSize(int cameraId, Parameters parameters) {

        // Set picture size
        String key = SettingUtil.getPictureSizeKey(cameraId);
        com.westalgo.factorycamera.util.Size picSize = mActivity.getSettingsManager()
                                               .getSize(SettingsManager.SCOPE_GLOBAL, key);
        if (picSize == null || mLsGlobalSettingChanged) {
            SettingUtil.initialCameraPictureSize(mActivity, parameters, cameraId);
            mLsGlobalSettingChanged = false;
        } else {
            parameters.setPictureSize(picSize.width, picSize.height);
        }

        android.hardware.Camera.Size pictureSize = parameters.getPictureSize();
        // Set preview size
        List<android.hardware.Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        android.hardware.Camera.Size previewSize = SettingUtil.getOptimalPreviewSize(mActivity, sizes,
                                       (double) pictureSize.width / pictureSize.height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        Log.v(TAG, "camera id :" + cameraId + " pictureSize "
              + pictureSize.width + "x" + pictureSize.height
              + " previewSize " + previewSize.width + "x"
              + previewSize.height);

        if (previewSize.width != 0 && previewSize.height != 0) {
            Log.v(TAG, "updating aspect ratio");
            Message msg = new Message();
            msg.what = PREVIEW_SIZE_CHANGED;
            msg.arg1 = previewSize.width;
            msg.arg2 = previewSize.height;
            mHandler.sendMessage(msg);
        }
    }

    private void updateParametersExposureCompensation(CameraProxy cameraDevice, Parameters parameters) {
        if (cameraDevice == null || parameters == null) {
            return;
        }
        SettingsManager settingsManager = mActivity.getSettingsManager();
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL,
                                       Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
            int value = settingsManager.getInteger(mActivity.getModuleScope(), Keys.KEY_EXPOSURE);
            int max = parameters.getMaxExposureCompensation();
            int min = parameters.getMinExposureCompensation();
            if (value >= min && value <= max) {
                parameters.setExposureCompensation(value);
            } else {
                Log.w(TAG, "invalid exposure range: " + value);
            }
        } else {
            // If exposure compensation is not enabled, reset the exposure compensation value.
            setExposureCompensation(cameraDevice, parameters,0);
        }
    }

    /**
     * Sets the exposure compensation to the given value and also updates
     * settings.
     *
     * @param value
     *            exposure compensation value to be set
     */
    public void setExposureCompensation(int value) {
        if(mMainCameraDevice != null && mMainParameters != null) {
            setExposureCompensation(mMainCameraDevice, mMainParameters, value);
            mMainCameraDevice.setParameters(mMainParameters);
        }
    }

    public void setExposureCompensation(CameraProxy cameraDevice, Parameters parameters, int value) {
        // OnResume the Device and Parameters may not prepared.
        // return would not effect anything, later setCameraParameters will update setExposureCompensation
        if (cameraDevice == null || parameters == null) {
            return;
        }

        final int min = parameters.getMinExposureCompensation();
        final int max = parameters.getMaxExposureCompensation();
        if (value >= min && value <= max) {
            parameters.setExposureCompensation(value);
            mActivity.getSettingsManager().set(mActivity.getModuleScope(), Keys.KEY_EXPOSURE, value);
        } else {
            Log.w(TAG, "invalid exposure range: " + value);
        }
    }

    public void setISO(String value) {
        setISO(mMainCameraDevice, mMainParameters, value);
    }

    public void setISO(CameraProxy cameraDevice, Parameters parameters, String value) {
        if (cameraDevice == null || parameters == null) {
            return;
        }
        Log.d(TAG, "setISO: " + value);
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.set(mActivity.getModuleScope(), Keys.KEY_ISO, value);
        if (value.equalsIgnoreCase(parameters.get(Keys.KEY_CURRENT_ISO))) {
            Log.d(TAG, "setISO, same value, ignore...");
            return;
        }
        parameters.set(Keys.KEY_CURRENT_ISO, value);
        Log.v(TAG, "after set, get current iso: " + parameters.get(Keys.KEY_CURRENT_ISO));
    }

    public void updateParametersISO(CameraProxy cameraDevice, Parameters parameters){
        SettingsManager settingsManager = mActivity.getSettingsManager();
        String supportedIsoValues = parameters.get(Keys.KEY_AVAILABLE_ISO);
        if (SettingUtil.SUPPORTED_ISO == null || !SettingUtil.SUPPORTED_ISO.equalsIgnoreCase(supportedIsoValues)) {
            Log.w(TAG, "ISO range: " + supportedIsoValues);
            SettingUtil.SUPPORTED_ISO = supportedIsoValues;
        }

        Log.v(TAG, "before set, get current iso: "+parameters.get(Keys.KEY_CURRENT_ISO));
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_ISO_ENABLED)) {
            String value = settingsManager.getString(mActivity.getModuleScope(), Keys.KEY_ISO);
            if (value != null) {
                setISO(cameraDevice, parameters, value);
            } else {
                setISO(cameraDevice, parameters, "auto");
            }
        } else {
            Log.w(TAG, "ISO setting is not enabled.");
        }
    }

    public void updateParametersFlashMode(CameraProxy cameraDevice, Parameters parameters) {
        boolean flashBackCamera = mActivity.getSettingsManager().getBoolean(
                                      SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA);
        if(flashBackCamera) {
            setParametersFlashMode(cameraDevice, parameters);
        }
    }

    public void updateParametersFlashMode() {
        if(mMainCameraDevice != null && mMainParameters != null) {
            setParametersFlashMode(mMainCameraDevice, mMainParameters);
            mMainCameraDevice.setParameters(mMainParameters);
        }
    }

    public void setParametersFlashMode(CameraProxy cameraDevice,
                                       Parameters parameters) {
        SettingsManager settingsManager = mActivity.getSettingsManager();

        String flashMode = settingsManager.getString(mActivity.getModuleScope(),
                           Keys.KEY_FLASH_MODE);
        if (CameraUtil.isSupported(flashMode,
                                   parameters.getSupportedFlashModes())) {
            parameters.setFlashMode(flashMode);
        }
    }
    /**************************** Capture Api ******************************/

    public void onShutterButtonClick() {
        if (mPaused || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == PREVIEW_STOPPED) || (null == mFocusManager)
                || (null == mUI.getMainSurfaceTexture()))
            return;

        mActivity.setShutterEnabled(false);

        // If the user wants to do a snapshot while the previous one is still
        // in progress, remember the fact and do it after we finish the previous
        // one and re-start the preview. Snapshot in progress also includes the
        // state that autofocus is focusing and a picture will be taken when
        // focus callback arrives.
        if ((mFocusManager.isFocusingSnapOnFinish() || mCameraState == SNAPSHOT_IN_PROGRESS)) {
            mSnapshotOnIdle = true;
            return;
        }
        mSnapshotOnIdle = false;
        mFocusManager.focusAndCapture(true);

    }

    @Override
    public boolean capture() {
        if (mMainCameraDevice == null || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA) {
            return false;
        }
        setCameraState(SNAPSHOT_IN_PROGRESS);
        mCaptureStartTime = System.currentTimeMillis();

        // Users should always get landscape photos while capturing by putting
        // device in landscape.
        mainJpegRotation = CameraUtil.getJpegRotation(mMainCameraId, mOrientation);
        mMainParameters.setRotation(mainJpegRotation);
        mMainCameraDevice.setParameters(mMainParameters);

        Log.v(TAG, "capture jpegRotation: " + mainJpegRotation + " screen(mOrientation):" + mOrientation);

        mNamedImages.nameNewImage(mCaptureStartTime);
        mNamedEntity = mNamedImages.getNextNameEntity();
        mMainCameraDevice.takePicture(mHandler, new ShutterCallback(), null, null, new MainJpegPictureCallback());

        return true;
    }

    private final class ShutterCallback implements CameraShutterCallback {

        public ShutterCallback() {
        }

        @Override
        public void onShutter(CameraProxy camera) {
        }
    }

    public void updateCaptureState() {
        if (mPaused) {
            return;
        }
        mActivity.setShutterEnabled(true);
        mFocusManager.resetTouchFocus();
        cancelAutoFocus();
        mFocusManager.updateFocusUI();

        setCameraState(IDLE);
    }

    public Parameters getMainParameters() {
        return mMainParameters;
    }


    /**************************** Focus Api ******************************/

    /**
     *
     * UI operation only for Main Camera
     */
    @Override
    public void onSingleTapUp(int x, int y) {
        if (mPaused || mMainCameraDevice == null
                || mCameraState == SNAPSHOT_IN_PROGRESS
                || mCameraState == SWITCHING_CAMERA
                || mCameraState == PREVIEW_STOPPED) {
            return;
        }
        mFocusManager.onSingleTapUp(x, y);
    }

    @Override
    public void setFocusParameters() {
        setCameraParameters(mMainCameraDevice, mMainParameters, UPDATE_PARAM_PREFERENCE);
    }

    @Override
    public void autoFocus() {
        if (mMainCameraDevice == null) {
            return;
        }
        Log.v(TAG, "Starting auto focus");
        mFocused = false;
        mMainCameraDevice.autoFocus(mHandler, mAutoFocusCallback);
        setCameraState(FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        if (mMainCameraDevice == null) {
            return;
        }
        Log.v(TAG, "cancelAutoFocus");
        mMainCameraDevice.cancelAutoFocus();
        if (mCameraState != SNAPSHOT_IN_PROGRESS) {
            setCameraState(IDLE);
        }
    }

    private final class AutoFocusCallback implements CameraAFCallback {

        @Override
        public void onAutoFocus(boolean focused, CameraProxy camera) {
            if (mFocused) {
                return;
            }
            setCameraState(IDLE);
            mFocusManager.onAutoFocus(focused);
            mFocused = focused;
        }
    }

    private final class AutoFocusMovingCallback implements CameraAFMoveCallback {

        @Override
        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {

            mFocusManager.onAutoFocusMoving(moving);
            // if(moving) {
            // mFocusManager.onAutoFocusMoving(moving, false);
            // } else {
            // if(mSubCameraDevice == null)return;
            // mSubCameraDevice.autoFocus(mHandler, mSubAutoFocusCallback2);
            // }
        }
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        setGlobalSettingChanged(true);
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            updateParametersFlashMode();
        }
    }

    @Override
    public boolean onBackPressed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();

        Parameters mainParameters = getMainParameters();
        if (CameraUtil.isSupported(Parameters.FLASH_MODE_AUTO, mainParameters.getSupportedFlashModes())) {
            bottomBarSpec.enableFlash = true;
        } else {
            bottomBarSpec.enableFlash = false;
        }
        bottomBarSpec.enableCamera = false;
        bottomBarSpec.cameraCallback = null;

        bottomBarSpec.minExposureCompensation = mainParameters.getMinExposureCompensation();
        bottomBarSpec.maxExposureCompensation = mainParameters.getMaxExposureCompensation();
        bottomBarSpec.exposureCompensationStep = mainParameters.getExposureCompensationStep();

        bottomBarSpec.enableExposureCompensation = true;
        bottomBarSpec.exposureCompensationSetCallback = new CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback() {
            @Override
            public void setExposure(int value) {
                setExposureCompensation(value);
            }
        };
        //ISO setting
        bottomBarSpec.enableISOSettings = false;
        bottomBarSpec.isoSetCallback = null;
        /*bottomBarSpec.isoSetCallback = new CameraAppUI.BottomBarUISpec.ISOSetCallback() {
            @Override
            public void setISOValue(String value) {
                Log.i(TAG, "ISOSetCallback, setISOValue: " + value);
                setISO(value);
            }
        };*/
        return bottomBarSpec;
    }

    @Override
    public String getModuleStringIdentifier() {
        // TODO Auto-generated method stub
        return PHOTO_MODULE_STRING_ID;
    }

    public class MainJpegPictureCallback implements CameraPictureCallback {

        @Override
        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            Log.d(TAG, "MainJpegPictureCallback, onPictureTaken");

            if (!mPaused && !CameraUtil.IS_RUN_BACKGRROUND) {
                setupPreview(true);
                setCaptureAndThumbnail();
            }

            final ExifInterface exif = Exif.getExif(originalJpegData);
            if (mShouldResizeTo16x9) {

            } else {
                int orientation = Exif.getOrientation(exif);
                // Calculate the width and the height of the jpeg.
                Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                int width, height;
                if (mShouldResizeTo16x9 && exifWidth != null && exifHeight != null) {
                    width = exifWidth;
                    height = exifHeight;
                } else {
                    Size s = camera.getParameters().getPictureSize();
                    if ((mainJpegRotation + orientation) % 180 == 0) {
                        width = s.width;
                        height = s.height;
                    } else {
                        width = s.height;
                        height = s.width;
                    }
                }
                Log.d(TAG, "picture size: "+width+"/"+height);
                String title = (mNamedEntity == null) ? null : mNamedEntity.title;
                long date = (mNamedEntity == null) ? -1 : mNamedEntity.date;

                if (title == null) {
                    Log.e(TAG, "Unbalanced name/data pair");
                } else {
                    if (date == -1) {
                        date = mCaptureStartTime;
                    }
                    mActivity.getMediaSaveService().addImage(originalJpegData, title, date, null,
                            width, height, orientation, exif, mOnMediaSavedListener,
                            mContentResolver, "jpeg");
                    Log.i(TAG, "OnPictureTaken, saved file: "+title);
                }
            }
        }
    }

    private final MediaSaveService.OnMediaSavedListener mOnMediaSavedListener = new MediaSaveService.OnMediaSavedListener() {
        @Override
        public void onMediaSaved(Uri uri) {
            if (uri != null) {
                CameraUtil.broadcastNewPicture(mActivity, uri);
            }
        }
    };

    public void setCaptureAndThumbnail() {
        updateCaptureState();
        // Set preview thumbnail
        mActivity.saveAndUpdateThumbnail();
        mActivity.showProgressView(false);
        mActivity.setUIClickEnable(true);
        // isInProgress = false;
    }
}
