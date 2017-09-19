package com.westalgo.factorycamera.module;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.view.OrientationEventListener;
import android.view.View;

import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.Exif;
import android.graphics.ImageFormat;
import com.westalgo.factorycamera.MediaSaveService;
import com.westalgo.factorycamera.Storage;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.app.CameraAppUI.BottomBarUISpec;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.exif.ExifInterface;
import com.westalgo.factorycamera.manager.CameraManager.CameraPictureCallback;
import com.westalgo.factorycamera.manager.CameraManager.CameraProxy;
import com.westalgo.factorycamera.module.DualBaseCameraModule.DualModeCallbackListener;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.util.CameraUtil.NamedImages.NamedEntity;

public class DualCameraModule extends DualBaseCameraModule implements
    ModuleController, DualModeCallbackListener, SettingsManager.OnSettingChangedListener {

    private static final Log.Tag TAG = new Log.Tag("DualCameraMod");

    public static final String DEPTH_MODULE_STRING_ID = "DualCameraModule";

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

    public DualCameraModule() {

    }

    @Override
    public void init(CameraActivity activity, View parent) {
        super.init(activity, parent, false);
        mActivity = activity;
        mContentResolver = activity.getContentResolver();
        setDualModeCallbackListener(this);
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
        // TODO Auto-generated method stub
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
        mActivity.saveThumbnail();
        //no need to show in progress text
        //mActivity.showProgressView(true);
        mActivity.setUIClickEnable(false);
        super.onShutterButtonClick();
    }

    /**
     * Sets the exposure compensation to the given value and also updates
     * settings.
     *
     * @param value
     *            exposure compensation value to be set
     */
    public void setExposureCompensation(int value) {

    }

    public void setCaptureAndThumbnail() {
        updateCaptureState();
        // Set preview thumbnail
        mActivity.updateThumbnail();
        mActivity.setUIClickEnable(true);
        mActivity.showProgressView(false);
    }

    public class MainJpegPictureCallback implements CameraPictureCallback {

        @Override
        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            // TODO Auto-generated method stub

            setupPreview(true);
            if (isCaptureComeBack) {
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
                    String path = title + SUFFIX_MAIN;
                    String depthPath = Storage.DIRECTORY + "/" + path + ".jpg";

                    if (date == -1) {
                        date = mCaptureStartTime;
                    }
                    mActivity.getMediaSaveService().addImage(originalJpegData,
                            path, date, null, width, height, 0, exif,
                            mOnMediaSavedListener, mContentResolver, Storage.IMAGE_TYPE_JPEG);
                }
            }

            isCaptureComeBack = true;

        }
    }

    public final class SubJpegPictureCallback implements CameraPictureCallback {

        @Override
        public void onPictureTaken(byte[] data, CameraProxy camera) {
            // TODO Auto-generated method stub
            if (mPaused) {
                return;
            }
            setupPreview(false);
            if (isCaptureComeBack) {
                setCaptureAndThumbnail();
            }

            int width, height;
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
            final ExifInterface exif = Exif.getExif(data);
            String title = (mNamedEntity == null) ? null : mNamedEntity.title;
            long date = (mNamedEntity == null) ? -1 : mNamedEntity.date;

            // Add for different Image Format
            int mSubPictureFormat = getSubParameters().getPictureFormat();
            switch (mSubPictureFormat) {
                case ImageFormat.JPEG:
                    if (title == null) {
                        Log.e(TAG, "Unbalanced name/data pair");
                    } else {
                        String path = title + SUFFIX_SUB;
                        String depthPath = Storage.DIRECTORY + "/" + path + ".jpg";
                        //setSubInfo(width, height, data, mSubJpegRotation);

                        if (date == -1) {
                            date = mCaptureStartTime;
                        }
                        mActivity.getMediaSaveService().addImage(data,
                                path, date, null, width, height, 0, exif,
                                mOnMediaSavedListener, mContentResolver, Storage.IMAGE_TYPE_JPEG);
                    }
                    break;
                case ImageFormat.NV21:
                    if (title == null) {
                        Log.e(TAG, "Unbalanced name/data pair");
                    } else {

                        byte[] yuvdata = CameraUtil.filterYuv420Data(data, width, height);
                        title = title + SUFFIX_SUB;
                        mActivity.getMediaSaveService().addImage(yuvdata, title, date,
                                null, width, height, 0, null,
                                mOnMediaSavedListener, mContentResolver, Storage.IMAGE_TYPE_DEBUGYUV);
                    }
                    break;
                default:
                    Log.e(TAG, "-->>onPictureTaken: Unknow SubCamera Image Format");
            }

            isCaptureComeBack = true;
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
        // TODO Auto-generated method stub
        SettingsManager settingsManager = mActivity.getSettingsManager();
        settingsManager.addListener(this);
    }

    public Bitmap getPreviewBitmap(int downsample) {
        return super.getPreviewBitmap(downsample);
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        super.onSettingChanged(settingsManager, key);
    }

    @Override
    public String getModuleStringIdentifier() {
        // TODO Auto-generated method stub
        return DEPTH_MODULE_STRING_ID;
    }

    @Override
    public BottomBarUISpec getBottomBarSpec() {
        // TODO Auto-generated method stub
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();

        bottomBarSpec.enableCamera = false;
        bottomBarSpec.cameraCallback = null;
        // Only support flash
        if (CameraUtil.isSupported(Parameters.FLASH_MODE_AUTO, getMainParameters().getSupportedFlashModes())) {
            bottomBarSpec.enableFlash = true;
        } else {
            bottomBarSpec.enableFlash = false;
        }
        bottomBarSpec.enableExposureCompensation = false;
        bottomBarSpec.exposureCompensationSetCallback = null;
        bottomBarSpec.enableISOSettings = false;
        bottomBarSpec.isoSetCallback = null;
        return bottomBarSpec;
    }


}
