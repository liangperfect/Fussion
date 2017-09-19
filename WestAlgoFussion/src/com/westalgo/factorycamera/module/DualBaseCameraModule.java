package com.westalgo.factorycamera.module;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.FrameLayout;

import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.FocusOverlayManager;
import com.westalgo.factorycamera.debug.Log;
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
import com.westalgo.factorycamera.settings.ProjectConfig;

import com.westalgo.factorycamera.supernight.SupernightManager;

public class DualBaseCameraModule implements PhotoController,
    FocusOverlayManager.Listener, SettingsManager.OnSettingChangedListener {
    private static final Log.Tag TAG = new Log.Tag("DualBaseCameraMod");
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

    private final AutoFocusCallback mMainAutoFocusCallback = new AutoFocusCallback();
    private final AutoFocusMovingCallback mMainAutoFocusMoveCallback = new AutoFocusMovingCallback();

    private CameraActivity mActivity;
    private Handler mHandler;
    private CameraProxy mMainCameraDevice;
    private Parameters mMainParameters;
    private int mMainCameraId = CameraHolder.CAMERA_MAIN_ID;

    private CameraProxy mSubCameraDevice;
    private Parameters mSubParameters;
    private int mSubCameraId = CameraHolder.CAMERA_SUB_ID;
    private boolean mPaused = false;
    private FrameLayout mRootView;

    private DualBaseCameraUI mUI;
    private OpenCameraThread mOpenCameraThread = null;

    private FocusOverlayManager mFocusManager;
    private boolean mMirror;
    private boolean mFocused = false;
    private boolean mIsTouchAF = false;// true for touch AF, false for CAF

    protected int mCameraState = PREVIEW_STOPPED;

    // when autofocus is focusing and a picture will be taken when
    // focus callback arrives.
    private boolean mSnapshotOnIdle = false;

    public NamedImages mNamedImages;
    public long mCaptureStartTime;
    private DualModeCallbackListener mDualModeCallbackListener;
    // Switch SuperNight mode Depth mode
    private boolean mIsSuperNight;
    // When setting changed
    private boolean mLsGlobalSettingChanged = false;
    //if need use nature correction.
    public static boolean useNatureCorrection = false;

    //protected DofManager dofManager;
    protected SupernightManager mSupernightManager;

    public DualBaseCameraModule() {
        mSupernightManager = new SupernightManager();
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
                Log.d(TAG, "---->>>handleMessage: ratio = " + ratio);
                mUI.updatePreviewAspectRatio(ratio);
                mUI.updateTextureView();
                break;
            case ON_PREVIEW_STARTED:
                onPreviewStarted();
                break;
            }
            super.handleMessage(msg);
        }
    }

    public void init(CameraActivity activity, View parent, boolean isSuperNight) {
        mActivity = activity;
        mRootView = (FrameLayout) parent;
        mIsSuperNight = isSuperNight;
        mHandler = new MainHandler();
        mUI = new DualBaseCameraUI(activity, this, mRootView);

        mActivity.getCameraAppUI().setPreviewStatusListener(mUI);
        mActivity.setShutterEnabled(true);

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
            initializeFocusManager();
            openMainCamera();
            openSubCamera();
            mHandler.sendEmptyMessageDelayed(CAMERA_OPEN_DONE, 100);
            startPreview(isMainCamera(mMainCameraId));
            startPreview(isMainCamera(mSubCameraId));
        }
    }

    private boolean isMainCamera(int cameraId) {
        return cameraId == CameraHolder.CAMERA_MAIN_ID ? true : false;
    }

    private void openMainCamera() {
        // We need to check whether the activity is paused before long
        // operations to ensure that onPause() can be done ASAP.
        if (mPaused) {
            return;
        }
        mMainCameraDevice = CameraHolder.instance().openMainCamera(mHandler,
                            mMainCameraId, mActivity.getCameraOpenErrorCallback());
        if (mMainCameraDevice == null) {
            Log.e(TAG, "Failed to open camera:" + mMainCameraId);
            mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            return;
        }
        mMainParameters = mMainCameraDevice.getParameters();
    }

    public CameraProxy getSubCameraProxy(){
        return mSubCameraDevice;
    }

    public void openSubCamera() {
        if (mPaused) {
            return;
        }
        mSubCameraDevice = CameraHolder.instance().openSubCamera(mHandler,
                           mSubCameraId, mActivity.getCameraOpenErrorCallback());
        if (mSubCameraDevice == null) {
            Log.e(TAG, "Failed to open camera:" + mSubCameraId);
            mHandler.sendEmptyMessage(OPEN_CAMERA_FAIL);
            return;
        }
        mSubParameters = mSubCameraDevice.getParameters();
        return;
    }

    public void resume() {
        // TODO Auto-generated method stub
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
        stopPreview(mSubCameraDevice);
        stopPreview(mMainCameraDevice);

        CameraHolder.instance().releaseSub();
        CameraHolder.instance().releaseMain();

        mMainCameraDevice = null;
        mSubCameraDevice = null;
    }

    public void destroy() {
        // TODO Auto-generated method stub
        SettingUtil.forceUpdateISO = true;
        //dofManager.release();

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
        //mActivity.setShutterEnabled(true);
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
        mDualModeCallbackListener.onCameraOpened();
        Log.v(TAG, "onCameraOpened");
    }


    /** Only called by UI thread. */
    public void setupPreview(boolean isMain) {
        Log.i(TAG, "setupPreview");
        if (isMain) {
            mFocusManager.resetTouchFocus();
        }
        startPreview(isMain);
    }

    /**
     * This can run on a background thread, so don't do UI updates here. Post
     * any view updates to MainHandler or do it on onPreviewStarted() .
     */
    @Override
    public void startPreview(boolean isMain) {
        Log.v(TAG, "startPreview isMain:" + isMain);
        if (isMain) {
            if (mPaused || (mMainCameraDevice == null)
                    || (mMainParameters == null)) {
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
        } else {
            if (mPaused || (mSubCameraDevice == null) || (mSubParameters == null)) {
                return;
            }
            SurfaceTexture st = null;
            if (mUI != null) {
                st = mUI.getSubSurfaceTexture();
            }
            mSubCameraDevice.setPreviewTexture(st);
            setCameraParameters(mSubCameraDevice, mSubParameters, UPDATE_PARAM_ALL);
            setDisplayOrientation(mSubCameraDevice, mSubCameraId);
            mSubCameraDevice.startPreview();
        }
    }

    public void stopPreview(CameraProxy cameraDevice) {
        if (cameraDevice != null) {
            Log.v(TAG, "stopPreview");
            cameraDevice.stopPreview();
        }
        setCameraState(PREVIEW_STOPPED);
        if (mFocusManager != null)
            mFocusManager.onPreviewStopped();
    }

    private void setDisplayOrientation(CameraProxy cameraDevice, int cameraId) {
        int mDisplayOrientation = CameraUtil.getCameraDisplayOrientation(mActivity.getAndroidContext(), cameraId);
        if (mUI != null) {
            mUI.setDisplayOrientation(mDisplayOrientation);
        }
        // Change the camera display orientation
        if (cameraDevice != null) {
            cameraDevice.setDisplayOrientation(mDisplayOrientation);
        }
        mFocusManager.setDisplayOrientation(mDisplayOrientation);
    }

    @Override
    public void stopPreview(boolean isMain) {
        // TODO Auto-generated method stub
        if (isMain) {
            if (mMainCameraDevice != null) {
                mMainCameraDevice.stopPreview();
            }
        } else {
            if (mSubCameraDevice != null) {
                mSubCameraDevice.stopPreview();
            }
        }
    }

    @Override
    public void onPreviewUIReady(boolean isMain) {
        // TODO Auto-generated method stub
        if (mPaused || mMainCameraDevice == null || mSubCameraDevice == null) {
            return;
        }
        if (isMain) {
            SurfaceTexture st = mUI.getMainSurfaceTexture();
            if (st == null) {
                Log.e(TAG, "startPreview: main surfaceTexture is not ready.");
                return;
            }
            mMainCameraDevice.setPreviewTexture(st);
        } else {
            SurfaceTexture st = mUI.getSubSurfaceTexture();
            if (st == null) {
                Log.e(TAG, "startPreview: sub surfaceTexture is not ready.");
                return;
            }
            mSubCameraDevice.setPreviewTexture(st);
        }
    }

    @Override
    public void onPreviewUIDestroyed(boolean isMain) {
        // TODO Auto-generated method stub
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

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        setGlobalSettingChanged(true);
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

        //turn on zsl or zsd
        boolean mZSLStatus = mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PREF_ZSL);
        setCameraZSLOrZSD(mZSLStatus, parameters);

        if (mIsSuperNight) {
            parameters.set(Keys.KEY_DUALCAM_MODE, "supernight");
        } else {
            parameters.set(Keys.KEY_DUALCAM_MODE, "depth");
            //get nature correction setting
            updateParamsNatureCorrection();
        }
    }

    private void updateCameraParametersPreference(CameraProxy cameraDevice, Parameters parameters) {
        boolean isMain = isMainCamera(cameraDevice.getCameraId());
        if (isMain) {
            if (mIsSuperNight) {
                parameters.setPictureFormat(ImageFormat.NV21);
            }else{
                parameters.setPictureFormat(ImageFormat.JPEG);
            }
        } else {
            boolean isNV21 = mActivity.getSettingsManager().getBoolean(
                    SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_SUBCAMEAR_IMAGE_FORMAT);
            if (isNV21) {
                parameters.setPictureFormat(ImageFormat.NV21);
            }
        }
        if (ProjectConfig.IMAGE_FORMAT == ImageFormat.NV21) {
            parameters.setPictureFormat(ImageFormat.NV21);
        }

        // Set Exposure Compensation
        updateParametersExposureCompensation(cameraDevice, parameters);
        // Set Flash Mode
        updateParametersFlashMode(cameraDevice, parameters);
        // Set ISO
        updateParametersISO(cameraDevice, parameters);
        // Set Focus mode
        updateParametersFocusMode(cameraDevice, parameters);
    }

    private void updateAutoFocusMoveCallback(CameraProxy cameraDevice, Parameters parameters) {
        if (!isMainCamera(cameraDevice.getCameraId())) {
            return;
        }
        if (parameters.getFocusMode() == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
            cameraDevice.setAutoFocusMoveCallback(mHandler, mMainAutoFocusMoveCallback);
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

        if (isMainCamera(cameraId)) {
           // Camera.Size size = parameters.getPictureSize();
           // dofManager.setImgInfo(size.width, size.height);
        }

        android.hardware.Camera.Size pictureSize = parameters.getPictureSize();
        // Set preview size
        List<android.hardware.Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        android.hardware.Camera.Size previewSize = SettingUtil
                .getOptimalPreviewSize(mActivity, sizes, (double) pictureSize.width / pictureSize.height);

        parameters.setPreviewSize(previewSize.width, previewSize.height);


        Log.v(TAG, "camera id :" + cameraId + " pictureSize "
              + pictureSize.width + "x" + pictureSize.height
              + " previewSize " + previewSize.width + "x"
              + previewSize.height);

        if (previewSize.width != 0 && previewSize.height != 0 && cameraId != CameraHolder.CAMERA_SUB_ID) {
            Log.v(TAG, "updating aspect ratio");
            Message msg = new Message();
            msg.what = PREVIEW_SIZE_CHANGED;
            msg.arg1 = previewSize.width;
            msg.arg2 = previewSize.height;
            mHandler.sendMessage(msg);
        }

    }

    private void updateParametersExposureCompensation(CameraProxy cameraDevice, Parameters parameters) {
        SettingsManager settingsManager = mActivity.getSettingsManager();
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
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

    // update nature correction setting
    private void updateParamsNatureCorrection() {
        SettingsManager settingsManager = mActivity.getSettingsManager();
        useNatureCorrection = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NATURE_CORRECTION);
        Log.v(TAG, "updateParamsNatureCorrection, useNatureCorrection = " + useNatureCorrection);
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
        if(mSubCameraDevice != null && mSubParameters != null) {
            setExposureCompensation(mSubCameraDevice, mSubParameters, value);
            mSubCameraDevice.setParameters(mSubParameters);
        }
    }

    public void setExposureCompensation(CameraProxy cameraDevice, Parameters parameters, int value) {
        // OnResume the Device and Parameters may not prepared.
        // return would not effect anything, later setCameraParameters will update setExposureCompensation
        if(cameraDevice == null || parameters == null) {
            return;
        }

        final int min = parameters.getMinExposureCompensation();
        final int max = parameters.getMaxExposureCompensation();
        if (value >= min && value <= max) {
            parameters.setExposureCompensation(value);
            if(isMainCamera(cameraDevice.getCameraId())) {
                SettingsManager settingsManager = mActivity.getSettingsManager();
                settingsManager.set(mActivity.getModuleScope(), Keys.KEY_EXPOSURE, value);
            }
        } else {
            Log.w(TAG, "invalid exposure range: " + value);
        }
    }

    public void setISO(String value) {
        if (mMainCameraDevice != null && mMainParameters != null) {
            setISO(mMainCameraDevice, mMainParameters, value);
        }
    }

    public void setISO(CameraProxy cameraDevice, Parameters parameters, String value) {
        if (cameraDevice == null || parameters == null) {
            return;
        }
        if (isMainCamera(cameraDevice.getCameraId())) {
            Log.d(TAG, "setISO: " + value);
            SettingsManager settingsManager = mActivity.getSettingsManager();
            settingsManager.set(mActivity.getModuleScope(), Keys.KEY_ISO, value);
            if (value.equalsIgnoreCase(parameters.get(Keys.KEY_CURRENT_ISO))) {
                Log.d(TAG, "setISO, same value, ignore...");
                return;
            }
            parameters.set(Keys.KEY_CURRENT_ISO, value);
            Log.d(TAG, "after set, get current iso: "+parameters.get(Keys.KEY_CURRENT_ISO));
        }
    }

    public void updateParametersISO(CameraProxy cameraDevice, Parameters parameters) {
        if (!isMainCamera(cameraDevice.getCameraId())) {
            return;
        }
        SettingsManager settingsManager = mActivity.getSettingsManager();
        String supportedIsoValues = parameters.get(Keys.KEY_AVAILABLE_ISO);
        //Log.w(TAG, "ISO range: " + supportedIsoValues);
        if (SettingUtil.SUPPORTED_ISO == null || !SettingUtil.SUPPORTED_ISO.equalsIgnoreCase(supportedIsoValues)) {
            Log.w(TAG, "ISO range: " + supportedIsoValues);
            SettingUtil.SUPPORTED_ISO = supportedIsoValues;
        }

        Log.d(TAG, "before set, get current iso: "+parameters.get(Keys.KEY_CURRENT_ISO));
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_ISO_ENABLED)) {
            String value = settingsManager.getString(mActivity.getModuleScope(), Keys.KEY_ISO);
            if (value != null) {
                setISO(value);
            } else {
                setISO("auto");
            }
        } else {
            Log.w(TAG, "ISO setting is not enabled.");
        }
    }

    public void updateParametersFocusMode(CameraProxy cameraDevice, Parameters parameters){
        Log.d(TAG, "updateParametersFocusMode, camera id: "+cameraDevice.getCameraId());
        boolean isMain = isMainCamera(cameraDevice.getCameraId());

        List<String> focusList = parameters.getSupportedFocusModes();

        if (focusList == null || focusList.size() <= 0) {
            Log.e(TAG, "updateParametersFocusMode, no focus mode supported!");
            return;
        }
        Log.i(TAG, "Support focus mode number: " + focusList.size()+", current: "+parameters.getFocusMode());
        if (focusList.size() > 1){
            Log.d(TAG, "Support multi focus mode.");
            if (isMain){
                parameters.setFocusAreas(mFocusManager.getFocusAreas());
                parameters.setMeteringAreas(mFocusManager.getMeteringAreas());
                parameters.setFocusMode(mFocusManager.getFocusMode());
            } else {
                parameters.setFocusAreas(mFocusManager.getFocusAreasForSub());
                parameters.setMeteringAreas(mFocusManager.getMeteringAreasForSub());
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
            }
        } else {
            Log.d(TAG, "Only support fixed focus mode.");
            parameters.setFocusMode(focusList.get(0));
        }
        updateAutoFocusMoveCallback(cameraDevice, parameters);
        Log.i(TAG, "updateParametersFocusMode end, current focus mode: "+parameters.getFocusMode());
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

        if(mSubCameraDevice != null && mSubParameters != null) {
            setParametersFlashMode(mSubCameraDevice, mSubParameters);
            mSubCameraDevice.setParameters(mSubParameters);
        }
    }

    public void setParametersFlashMode(CameraProxy cameraDevice, Parameters parameters) {
        SettingsManager settingsManager = mActivity.getSettingsManager();

        String flashMode = settingsManager.getString(mActivity.getModuleScope(), Keys.KEY_FLASH_MODE);
        if (CameraUtil.isSupported(flashMode, parameters.getSupportedFlashModes())) {
            parameters.setFlashMode(flashMode);
        }
    }
    /**************************** Capture Api ******************************/

    public void onShutterButtonClick() {
        if (mPaused || (mCameraState == SWITCHING_CAMERA)
                || (mCameraState == PREVIEW_STOPPED) || (null == mFocusManager)
                || (null == mUI.getMainSurfaceTexture())
                || (null == mUI.getSubSurfaceTexture()))
            return;
        Log.d(TAG, "[TIME_TAG]APP:onShutterButtonClick, click button disable--> click start!");

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
        //in dual camera mode , auto focus is not need before capture
        boolean needFocus = mActivity.getCurrentModuleIndex() != ModulesInfo.DUAL_CAMERA_MODE;
        mFocusManager.focusAndCapture(false);
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
        //mOrientation = 270 - mOrientation;
        int mainJpegRotation = CameraUtil.getJpegRotation(mMainCameraId, mOrientation);
        mMainParameters.setRotation(mainJpegRotation);
        mMainParameters.setPictureFormat(ImageFormat.NV21);
        mMainCameraDevice.setParameters(mMainParameters);

        int subRotation = CameraUtil.getJpegRotation(mSubCameraId, mOrientation);
        mSubParameters.setRotation(subRotation);
        mSubParameters.setPictureFormat(ImageFormat.NV21);
        mSubCameraDevice.setParameters(mSubParameters);

        Log.d(TAG, "capture jpegRotation(main): " + mainJpegRotation
              + " jpegRotation(sub):" + subRotation
              + " screen(mOrientation):" + mOrientation);

        mNamedImages.nameNewImage(mCaptureStartTime);

        if (mDualModeCallbackListener != null) {
            mMainCameraDevice.takePicture(mHandler, new ShutterCallback(),
                                          null, null,
                                          mDualModeCallbackListener.captureMain(mainJpegRotation, mOrientation));
            mSubCameraDevice.takePicture(mHandler, null, null, null,
                                         mDualModeCallbackListener.captureSub(subRotation, mOrientation));
        }

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

    public Parameters getSubParameters() {
        return mSubParameters;
    }

    public void setDualModeCallbackListener(DualModeCallbackListener listener) {
        mDualModeCallbackListener = listener;
    }

    public interface DualModeCallbackListener {
        public CameraPictureCallback captureMain(int rotation, int screenOrientation);

        public CameraPictureCallback captureSub(int rotation, int screenOrientation);

        public void onCameraOpened();
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
        // TODO Auto-generated method stub
        setCameraParameters(mMainCameraDevice, mMainParameters, UPDATE_PARAM_PREFERENCE);
        if (mFocusManager.getFocusMode() == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
            setCameraParameters(mSubCameraDevice, mSubParameters, UPDATE_PARAM_PREFERENCE);
        }
    }

    @Override
    public void autoFocus() {
        if (mMainCameraDevice == null) {
            return;
        }
        Log.v(TAG, "Starting auto focus");
        mFocused = false;
        mMainCameraDevice.autoFocus(mHandler, mMainAutoFocusCallback);
        setCameraState(FOCUSING);
    }

    @Override
    public void cancelAutoFocus() {
        if (mMainCameraDevice == null || mSubCameraDevice == null)
            return;

        mMainCameraDevice.cancelAutoFocus();
        //cancel focus if sub camera supports af.
        if (mSubParameters.getSupportedFocusModes() != null &&
                mSubParameters.getSupportedFocusModes().size() > 1) {
            mSubCameraDevice.cancelAutoFocus();
        }
        if (mCameraState != SNAPSHOT_IN_PROGRESS) {
            setCameraState(IDLE);
        }
    }

    private boolean subAutoFocus() {
        if (mSubCameraDevice == null) {
            return false;
        }
        // set sub focus callback if support auto focus
        if (mSubParameters.getSupportedFocusModes() != null &&
                mSubParameters.getSupportedFocusModes().size() > 1) {
            mSubCameraDevice.autoFocus(mHandler, mSubAutoFocusCallback);
            return true;
        } else {
            Log.i(TAG, "subAutoFocus, a fixed sub camera.");
            return false;
        }
    }

    private final class AutoFocusCallback implements CameraAFCallback {

        @Override
        public void onAutoFocus(boolean focused, CameraProxy camera) {
            mIsTouchAF = true;
            Log.v(TAG, "onAutoFocus, focused: "+focused);
            if (mFocused) {
                return;
            }
            setCameraState(IDLE);
            if (focused) {
                setCameraParameters(mSubCameraDevice, mSubParameters, UPDATE_PARAM_PREFERENCE);
                //if fixed sub camera, update focused UI.
                if (!subAutoFocus()) {
                    mFocusManager.onAutoFocus(focused);
                }
            }
            mFocused = focused;
        }
    }

    private final class AutoFocusMovingCallback implements CameraAFMoveCallback {

        @Override
        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
            // mFocusManager.onAutoFocusMoving(moving);
            Log.v(TAG, "onAutoFocusMoving, moving: "+moving);
            if (!moving) {
                mIsTouchAF = false;
                // if fixed camera, update focused UI directly.
                if (!subAutoFocus()) {
                    setCameraState(IDLE);
                    mFocusManager.onAutoFocusMoving(moving, true);
                }
            } else {
                mFocusManager.onAutoFocusMoving(moving, false);
            }
        }
    }

    // This callback for main is autoFocus, sub is autoFocus
    private CameraAFCallback mSubAutoFocusCallback = new CameraAFCallback() {

        @Override
        public void onAutoFocus(boolean focused, CameraProxy camera) {

            Log.v(TAG, "mSubAutoFocusCallback, onAutoFocus:" + focused);
            // mFocusManager.onAutoFocus(focused && mFocused);
            if (mIsTouchAF) {
                mFocusManager.onAutoFocus(focused);
            } else {
                setCameraState(IDLE);
                mFocusManager.onAutoFocusMoving(false, focused);
            }
        }
    };

    private void setCameraZSLOrZSD(boolean mZSLStatus, Parameters parameters) {
        //set parameters from xml file
        ProjectConfig.getConfig().setExtraParameters(parameters);
        if (mZSLStatus) {
            if (CameraUtil.isMTK()) {
                parameters.set(Keys.KEY_ZSD, "on");
            }else {
                parameters.set(Keys.KEY_ZSL, "on");
            }
        } else {
            if (CameraUtil.isMTK()){
                parameters.set(Keys.KEY_ZSD, "off");
            }else {
                parameters.set(Keys.KEY_ZSL, "off");
            }
        }
    }
}
