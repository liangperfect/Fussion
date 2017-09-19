package com.westalgo.factorycamera.module;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.view.OrientationEventListener;
import android.view.View;

import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.Exif;

import android.graphics.ImageFormat;

import com.westalgo.factorycamera.Storage;
import com.westalgo.factorycamera.MediaSaveService;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.app.CameraAppUI.BottomBarUISpec;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.exif.ExifInterface;
import com.westalgo.factorycamera.manager.CameraManager.CameraPictureCallback;
import com.westalgo.factorycamera.manager.CameraManager.CameraProxy;
import com.westalgo.factorycamera.module.DualBaseCameraModule.DualModeCallbackListener;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.util.CameraUtil.NamedImages.NamedEntity;
import com.westalgo.factorycamera.R;
import com.dcs.verify.DCSVerifyEngineCallback;
import com.dcs.verify.DCSVerifyItem;
import com.dcs.verify.DCSVerifyService;
import com.dcs.verify.DCSVerifyService.DCSVerifyCallBack;
import com.dcs.verify.DCSVerifyServiceHelper;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.settings.ProjectConfig;
import com.westalgo.factorycamera.util.VerifyHelper;


import java.nio.ByteBuffer;

import com.westalgo.factorycamera.supernight.SupernightItem;

public class DualVerifyModule extends DualBaseCameraModule implements
        ModuleController, DualModeCallbackListener, SettingsManager.OnSettingChangedListener {

    private static final Log.Tag TAG = new Log.Tag("DualVerifyMode");

    private static final String SUFFIX_MAIN = "_main";
    private static final String SUFFIX_SUB = "_sub";

    private ContentResolver mContentResolver;
    private CameraActivity mActivity;
    private boolean mShouldResizeTo16x9 = false;
    private boolean mPaused = false;

    // The value for cameradevice.CameraSettings.setPhotoRotationDegrees.
    private int mMainJpegRotation;
    private int mSubJpegRotation;
    private int mScreenOrientation;
    private NamedEntity mNamedEntity;

    private boolean isCaptureComeBack = false;

    //verify item
    DCSVerifyItem verifyItem;
    private boolean isSetResultCallBack = false;
    ProgressDialog progressDialog;
    private int correctionMode;

    //Supernight item
    SupernightItem mSupernightItem;

    public DualVerifyModule() {

    }

    @Override
    public void init(CameraActivity activity, View parent) {
        super.init(activity, parent, false);
        mActivity = activity;
        mContentResolver = activity.getContentResolver();
        setDualModeCallbackListener(this);
        correctionMode = mActivity.getSettingsManager()
                .getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_CORRECTION, 0);
    }

    @Override
    public void resume() {
        super.resume();
        mPaused = false;
    }

    @Override
    public void pause() {
        super.pause();
        mPaused = true;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }

        // Flip to counter-clockwise orientation.
        // int orien = (360 - orientation) % 360;
        super.onOrientationChanged(orientation);
    }

    @Override
    public boolean onBackPressed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onShutterButtonClick() {
        super.onShutterButtonClick();
    }

    /**
     * Sets the exposure compensation to the given value and also updates
     * settings.
     *
     * @param value exposure compensation value to be set
     */
    public void setExposureCompensation(int value) {

    }

    /****************************
     * Capture api
     ******************************/

    public class MainJpegPictureCallback implements CameraPictureCallback {

        @Override
        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            //luke do not start preview after capture
            setupPreview(true);

            // final ExifInterface exif = Exif.getExif(originalJpegData);
            if (mShouldResizeTo16x9) {

            } else {
                //int orientation = Exif.getOrientation(exif);
                int orientation = 0; //Exif.getOrientation(exif);
                // Calculate the width and the height of the jpeg.
                // Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                //Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                Integer exifWidth = 4160;//exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = 3136;//exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                int width, height;
                if (mShouldResizeTo16x9 && exifWidth != null
                        && exifHeight != null) {
                    width = exifWidth;
                    height = exifHeight;
                } else {
                    Size s = camera.getParameters().getPictureSize();
                    if ((mMainJpegRotation + orientation) % 180 == 0) {
                        width = s.width;
                        height = s.height;
                    } else {
                        width = s.height;
                        height = s.width;
                    }
                }
                String title = (mNamedEntity == null) ? null : mNamedEntity.title;
                long date = (mNamedEntity == null) ? -1 : mNamedEntity.date;

                if (title == null) {
                    Log.e(TAG, "Unbalanced name/data pair");
                } else {
                    String mainTitle = title + SUFFIX_MAIN;
                    if (date == -1) {
                        date = mCaptureStartTime;
                    }

                    //set main item info
                    Log.i(TAG, "MainJpegPictureCallback: width:" + width + " height:" + height);
                    setMainInfo(width, height, originalJpegData, mMainJpegRotation, correctionMode);
                    //setFileInfo(mainTitle, date, 0, originalJpegData.length);
                    //luke save picture is debug mode
                    /*if (CameraUtil.DEBUG_MODE) {
                        mActivity.getMediaSaveService().addImage(originalJpegData, mainTitle,
                                date, null, width, height, 0, exif,
                                mOnMediaSavedListener, mContentResolver, "jpeg");
                    }*/
                }
            }
        }
    }

    public final class SubJpegPictureCallback implements CameraPictureCallback {

        @Override
        public void onPictureTaken(byte[] data, CameraProxy camera) {
            if (mPaused) {
                return;
            }
            //luke do not start preview after capture
            setupPreview(false);

            int width, height;
            //final ExifInterface exif = Exif.getExif(data);
            if (mShouldResizeTo16x9) {
                width = 0;
                height = 0;
            } else {
                Size s = camera.getParameters().getPictureSize();
                if (mSubJpegRotation == 90 | mSubJpegRotation == 270) {
                    width = s.height;
                    height = s.width;
                } else {
                    width = s.width;
                    height = s.height;
                }
            }

            String title = (mNamedEntity == null) ? null : mNamedEntity.title;
            long date1 = (mNamedEntity == null) ? -1 : mNamedEntity.date;

            if (title == null) {
                Log.e(TAG, "Unbalanced name/data pair");
            } else {
                String subTitle = title + SUFFIX_SUB;
                if (date1 == -1) {
                    date1 = mCaptureStartTime;
                }

                Log.e(TAG, "westalgo :into setSubInfo  data size:" + data.length);
                //set sub item info
                setSubInfo(width, height, data, mSubJpegRotation, ImageFormat.JPEG);

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

    private final MediaSaveService.OnMediaSavedListener mOnMediaSavedSubListener = new MediaSaveService.OnMediaSavedListener() {
        @Override
        public void onMediaSaved(Uri uri) {
            if (uri != null) {
                CameraUtil.broadcastNewPicture(mActivity, uri);
            }
        }
    };


    @Override
    public boolean capture() {
        mSupernightItem = SupernightItem.createInstance();
        return super.capture();
    }

    @Override
    public CameraPictureCallback captureMain(int rotation, int screenOrientation) {
        isCaptureComeBack = false;
        mNamedEntity = mNamedImages.getNextNameEntity();
        mMainJpegRotation = rotation;
        mScreenOrientation = screenOrientation;
        return new MainJpegPictureCallback();
    }

    @Override
    public CameraPictureCallback captureSub(int rotation, int screenOrientation) {
        mSubJpegRotation = rotation;
        return new SubJpegPictureCallback();
    }

    @Override
    public void onCameraOpened() {
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.addListener(this);
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        super.onSettingChanged(settingsManager, key);
        Log.d(TAG, "onSettingChanged");
        correctionMode = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_CORRECTION, 0);
    }

    private void setFileInfo(String title, long date, int orientation, int length) {
        verifyItem.title = title;
        verifyItem.date = date;
        verifyItem.length = length;
        verifyItem.orientation = orientation;
    }

    public void setMainInfo(int width, int height, byte[] data, int orientation, int correctionMode) {

        mSupernightItem.setMainW(width);
        mSupernightItem.setMainH(height);
        ByteBuffer mainYBuffer = ByteBuffer.allocate(mSupernightItem.getMainW() * mSupernightItem.getMainH());
        ByteBuffer mainUVBuffer = ByteBuffer.allocate(mSupernightItem.getMainW() * mSupernightItem.getMainH() / 2);
        mainYBuffer.put(data, 0, mSupernightItem.getMainW() * mSupernightItem.getMainH());
        mainUVBuffer.put(data, mSupernightItem.getMainW() * mSupernightItem.getMainH(), mSupernightItem.getMainW() * mSupernightItem.getMainH() / 2);
        mSupernightItem.setMainY(mainYBuffer.array());
        Log.e(TAG, "westalgo:mainYBuffer.array().lenght->" + mainYBuffer.array().length);
        mSupernightItem.setMainUV(mainUVBuffer.array());
        Log.e(TAG, "westalgo:mainUVBuffer.array()t->" + mainUVBuffer.array());
        mSupernightItem.setRgbIso(800);
        mSupernightItem.setMonoIso(800);
        Log.e(TAG, "westalgo : data size" + data.length + "   buffer size:" + mSupernightItem.getMainW() * mSupernightItem.getMainH() + " mainYBuffer size:" + mainYBuffer.array().length + "  mainUVBuffer:" + mainUVBuffer.array().length);
        mSupernightItem.setIsDatOK();
        executeVerify(mSupernightItem);
    }

    public void setSubInfo(int width, int height, byte[] data, int orientation, int mSubPictureFormat) {

        Log.e(TAG, "westalgo : data size" + data.length + "   buffer size:" + mSupernightItem.getAuxW() * mSupernightItem.getAuxH());
        mSupernightItem.setAuxW(width);
        mSupernightItem.setAuxH(height);
        ByteBuffer auxYBuffer = ByteBuffer.allocate(mSupernightItem.getAuxW() * mSupernightItem.getAuxH());
        ByteBuffer auxUVBuffer = ByteBuffer.allocate(mSupernightItem.getAuxW() * mSupernightItem.getAuxH() / 2);
        auxYBuffer.put(data, 0, mSupernightItem.getAuxW() * mSupernightItem.getAuxH());
        auxUVBuffer.put(data, mSupernightItem.getAuxW() * mSupernightItem.getAuxH(), mSupernightItem.getAuxW() * mSupernightItem.getAuxH() / 2);
        mSupernightItem.setAuxY(auxYBuffer.array());
        Log.e(TAG, "westalgo:uxYBuffer.array().length->" + auxYBuffer.array().length);
        mSupernightItem.setAuxUV(auxUVBuffer.array());
        Log.e(TAG, "westalgo:auxUVBuffer.array()->" + auxUVBuffer.array().length);
        Log.e(TAG, "westalgo : data size" + data.length + "   buffer size:" + mSupernightItem.getMainW() * mSupernightItem.getMainH() + " mainYBuffer size:" + auxYBuffer.array().length + "  mainUVBuffer:" + auxUVBuffer.array().length);
        //pass supernight data
        mSupernightManager.setImgInfo(width, height);
        mSupernightItem.setIsDatOK();
        executeVerify(mSupernightItem);
    }

    public void executeVerify(SupernightItem supernightItem) {
        if (null != mSupernightItem) {
            mActivity.setShutterEnabled(false);
            mActivity.setUIClickEnable(false);
            //listener
            Log.e(TAG, "westalog: exe mSupernightManager.setData");
            mSupernightManager.setSupernihtListener(new DcsSupernightListener());
            mSupernightManager.setData(supernightItem, ImageFormat.NV21);
            mSupernightManager.process();
        }

    }

    public class DcsSupernightListener implements com.westalgo.factorycamera.supernight.SupernightManager.SupernightListener {

        public void onProcessFinish(String filePath) {
            //DCSVerifyItem item = dofManager.getImageItem();
            //Storage.addImage(mContentResolver, item.title, item.date, null, item.orientation,
            //        item.length, filePath, item.mMainImgW, item.mMainImgH, "jpeg");
            mActivity.updateCurrentThumbnail();
            setCaptureAndThumbnail();

        }
    }


    public void setCaptureAndThumbnail() {
        updateCaptureState();
        // Set preview thumbnail
        mActivity.updateThumbnail();
        mActivity.setShutterEnabled(true);
        mActivity.setUIClickEnable(true);
        mActivity.showProgressView(false);
    }

    /*
        private DofManager.DofListener dofListener = new DofManager.DofListener() {
            @Override
            public void onProcessFinish(String filePath) {
                DCSVerifyItem item = dofManager.getImageItem();
                Storage.addImage(mContentResolver, item.title, item.date, null, item.orientation,
                        item.length, filePath, item.mMainImgW, item.mMainImgH, "jpeg");
                mActivity.updateCurrentThumbnail();
                setCaptureAndThumbnail();
            }
        };
    */
    private VerifyHelper.Callback verifyCallback = new VerifyHelper.Callback() {
        public void onVerifyFinished(int result, float[] info) {
            Log.d(TAG, "verifyCallback result:" + result);
            dismissProgressDialog();
            if (result <= 0 && result != -1) {
                //write data and flag
                CameraUtil.execCommand(CameraUtil.SYS_DUALCAMERA_CALI, 1);
            } else if (result == -1) {
                //write flag
                CameraUtil.execCommand(CameraUtil.SYS_DUALCAMERA_CALI, 2);
            } else {
                //nothing
            }
            //mActivity.updateResult(verifyItems);
            mActivity.showVerifyDialog(result);
        }
    };


    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(mActivity);
        }
        progressDialog.setMessage(mActivity.getResources().getString(R.string.wait_for_callback));
        progressDialog.setCancelable(false);
        progressDialog.setOnCancelListener(null);
        progressDialog.show();
    }

    private void updateProgressDialog(String msg) {
        if (progressDialog != null) {
            progressDialog.setMessage(msg);
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    public Bitmap getPreviewBitmap(int downsample) {
        return super.getPreviewBitmap(downsample);
    }

    /**
     * Returns a unique string which identifies this module.
     * This string is used by the SettingsManager to scope settings
     * specific to each module.
     */
    public String getModuleStringIdentifier() {
        return "Test";
    }

    @Override
    public BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();

        bottomBarSpec.enableCamera = false;
        bottomBarSpec.cameraCallback = null;
        bottomBarSpec.enableFlash = false;
        bottomBarSpec.enableExposureCompensation = true;
        bottomBarSpec.exposureCompensationSetCallback = new CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback() {
            @Override
            public void setExposure(int value) {
                setExposureCompensation(value);
            }
        };
        bottomBarSpec.minExposureCompensation = getMainParameters().getMinExposureCompensation();
        bottomBarSpec.maxExposureCompensation = getMainParameters().getMaxExposureCompensation();
        bottomBarSpec.exposureCompensationStep = getMainParameters().getExposureCompensationStep();

        bottomBarSpec.enableISOSettings = true;
        bottomBarSpec.isoSetCallback = new CameraAppUI.BottomBarUISpec.ISOSetCallback() {
            @Override
            public void setISOValue(String value) {
                Log.i(TAG, "ISOSetCallback, setISOValue: " + value);
                setISO(value);
            }
        };
        return bottomBarSpec;
    }

}
