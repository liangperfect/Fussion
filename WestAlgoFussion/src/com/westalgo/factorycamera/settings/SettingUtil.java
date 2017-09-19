package com.westalgo.factorycamera.settings;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.manager.CameraHolder;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

public class SettingUtil {
    private static final Log.Tag TAG = new Log.Tag("SettingUtil");
    public static final float ratio4to3 = (float)4/3;
    public static final float ratio16to9 = (float)16/9;

    //the supported ISO config
    public static String SUPPORTED_ISO;
    //force udpate iso setting items
    public static boolean forceUpdateISO = false;

    public static void initialCameraPictureSize(CameraActivity activity,
            Parameters parameters, int cameraId) {
        List<Size> supported = parameters.getSupportedPictureSizes();
        if (supported == null)
            return;
        Comparator<Size> sizeComparator = new Comparator<Size>() {

            @Override
            public int compare(Size lhs, Size rhs) {
                int leftArea = lhs.width * lhs.height;
                int rightArea = rhs.width * rhs.height;
                return rightArea-leftArea ;
            }
        };
        boolean isAspect16to9 = activity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO_BACK);

        Collections.sort(supported, sizeComparator);
        Size pictureSize = null;
        float ratio = ratio4to3;
        if(isAspect16to9) {
            ratio = ratio16to9;
        }

        for (Size size : supported) {
            float aspectRatio = size.width / (float) size.height;
            // Allow for small rounding errors in aspect ratio.
            if (Math.abs(aspectRatio - ratio) < 0.01) {
                pictureSize = size;
                break;
            }
        }

        parameters.setPictureSize(pictureSize.width, pictureSize.height);

        String sizeSetting = SettingUtil.sizeToSetting(pictureSize);
        String key = getPictureSizeKey(cameraId);
        if (key != null) {
            Log.v(TAG, "camera id :" + cameraId + " pictureSize "
                  + pictureSize.width + "x" + pictureSize.height);
            activity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL,
                                              key, sizeSetting);
        }
    }

    public static String getPictureSizeKey(int cameraId) {
        if (cameraId == CameraHolder.CAMERA_MAIN_ID)
            return Keys.KEY_PICTURE_SIZE_BACK_MAIN;
        if (cameraId == CameraHolder.CAMERA_SUB_ID)
            return Keys.KEY_PICTURE_SIZE_BACK_SUB;
        if (cameraId == CameraHolder.CAMERA_FRONT_ID)
            return Keys.KEY_PICTURE_SIZE_FRONT;
        else {
            Log.e(TAG, "getPictureSizeKey error cameraid:" + cameraId);
            return null;
        }
    }

    public static String sizeToSetting(Size size) {
        return ((Integer) size.width).toString() + "x"
               + ((Integer) size.height).toString();
    }

    private static Point getDefaultDisplaySize(Activity activity, Point size) {
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        return size;
    }

    public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio) {

        Point[] points = new Point[sizes.size()];

        int index = 0;
        for (Size s : sizes) {
            points[index++] = new Point(s.width, s.height);
        }

        int optimalPickIndex = getOptimalPreviewSize(currentActivity, points,
                               targetRatio);
        return (optimalPickIndex == -1) ? null : sizes.get(optimalPickIndex);
    }

    public static int getOptimalPreviewSize(Activity currentActivity,
                                            Point[] sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.01;
        if (sizes == null)
            return -1;

        int optimalSizeIndex = -1;
        double minDiff = Double.MAX_VALUE;

        // For now, just get the screen size.
        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int targetHeight = Math.min(point.x, point.y);
        // Try to find an size match aspect ratio and size
        for (int i = 0; i < sizes.length; i++) {
            Point size = sizes[i];
            double ratio = (double) size.x / size.y;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.y - targetHeight) < minDiff) {
                optimalSizeIndex = i;
                minDiff = Math.abs(size.y - targetHeight);
            }
        }
        return optimalSizeIndex;
    }

    public static String[] getStringValueSplitWhiteSpace(String values) {
        if (values != null && values != "") {
            return values.split("\\,");
        }
        return null;
    }
}
