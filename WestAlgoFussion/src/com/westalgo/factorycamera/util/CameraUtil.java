/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.westalgo.factorycamera.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.CameraActivity;
import com.westalgo.factorycamera.Storage;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.manager.CameraHolder;

/**
 * Collection of utility functions used in this package.
 */
public class CameraUtil {
    private static final Log.Tag TAG = new Log.Tag("CameraUtil");

    /**
     * Width of touch AF region in [0,1] relative to shorter edge of the current
     * crop region. Multiply this number by the number of pixels along the
     * shorter edge of the current crop region's width to get a value in pixels.
     *
     * <p>
     * This value has been tested on Nexus 5 and Shamu, but will need to be
     * tuned per device depending on how its ISP interprets the metering box and weight.
     * </p>
     *
     * <p>
     * Values prior to L release:
     * Normal mode: 0.125 * longest edge
     * Gcam: Fixed at 300px x 300px.
     * </p>
     */
    public static final float AF_REGION_BOX = 0.2f;

    /**
     * Width of touch metering region in [0,1] relative to shorter edge of the
     * current crop region. Multiply this number by the number of pixels along
     * shorter edge of the current crop region's width to get a value in pixels.
     *
     * <p>
     * This value has been tested on Nexus 5 and Shamu, but will need to be
     * tuned per device depending on how its ISP interprets the metering box and weight.
     * </p>
     *
     * <p>
     * Values prior to L release:
     * Normal mode: 0.1875 * longest edge
     * Gcam: Fixed at 300px x 300px.
     * </p>
     */
    public static final float AE_REGION_BOX = 0.3f;

    /** Duration to hold after manual tap to focus. */
    public static final int FOCUS_HOLD_MILLIS = 3000;

    /** See android.hardware.Camera.ACTION_NEW_PICTURE. */
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";

    private static ImageFileNamer sImageFileNamer;
    private static float sPixelDensity = 1;

    //enable calibration
    public static final String SYS_DUALCAMERA_CALI = "setprop sys.dualcamera.cali ";
    //should off verify debug mode(compress photo and cali data to file)
    public static final String SYS_CAMERA_VERIFY_DEBUG = "getprop persist.camera.verify_debug_off";
    //enable AE sync
    public static final String SYS_DUALCA_AE_SWITCH = "setprop persist.sys.dualcam.aesync ";
    //enable Frame sync
    public static final String SYS_DUALCA_FRAME_SWITCH = "setprop persist.sys.dualcam.zsl.sync ";
    public static boolean DEBUG_MODE = true;

    /**
     * Indicate when IS_HAL_SUPERNIGHT = false, IS_RUN_BACKGRROUND = false, fusion data encode by hardware or software
     */
    public static final boolean IS_HARDWARE_ENCODE = true;
    /**
     * Now run front, the app will response until super night feature fusion complete
     */
    public  static final boolean IS_RUN_BACKGRROUND = false;
    public static final String MTK_PLATFORM = "MT";

    public static void execCommand(String command, boolean status) {
        int switchIsOn = status ? 1 : 0;
        execCommand(command,switchIsOn);
    }

    public static void execCommand(String command, int switchIsOn) {
        Process proc = null;
        BufferedReader bis = null;
        try {
            Log.d(TAG, "--->>execCommand command + switchIsOn = " + command + switchIsOn);
            proc = Runtime.getRuntime().exec(command + switchIsOn);
            bis = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            bis.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
                proc.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
        sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }

    public static int dpToPixel(int dp) {
        return Math.round(sPixelDensity * dp);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror,
                                     int displayOrientation, Rect previewRect) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);

        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // We need to map camera driver coordinates to preview rect coordinates
        Matrix mapping = new Matrix();
        mapping.setRectToRect(new RectF(-1000, -1000, 1000, 1000),
                              rectToRectF(previewRect), Matrix.ScaleToFit.FILL);
        matrix.setConcat(mapping, matrix);
    }

    public static Rect rectFToRect(RectF rectF) {
        Rect rect = new Rect();
        rectFToRect(rectF, rect);
        return rect;
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    /**
     * Gets the theme color of a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return theme color of the mode if input index is valid, otherwise 0
     */
    public static int getCameraThemeColorId(int modeIndex, Context context) {

        // Find the theme color using id from the color array
        TypedArray colorRes = context.getResources().obtainTypedArray(
                                  R.array.camera_mode_theme_color);
        if (modeIndex >= colorRes.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return colorRes.getResourceId(modeIndex, 0);
    }

    /**
     * Gets the mode icon resource id of a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return icon resource id if the index is valid, otherwise 0
     */
    public static int getCameraModeIconResId(int modeIndex, Context context) {
        // Find the camera mode icon using id
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(
                                          R.array.camera_mode_icon);
        if (modeIndex >= cameraModesIcons.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return cameraModesIcons.getResourceId(modeIndex, 0);
    }

    /**
     * Gets the mode text of a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return mode text if the index is valid, otherwise a new empty string
     */
    public static String getCameraModeText(int modeIndex, Context context) {
        // Find the camera mode icon using id
        String[] cameraModesText = context.getResources().getStringArray(
                                       R.array.camera_mode_text);
        if (modeIndex < 0 || modeIndex >= cameraModesText.length) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return new String();
        }
        return cameraModesText[modeIndex];
    }

    /**
     * Gets the mode content description of a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return mode content description if the index is valid, otherwise a new
     *         empty string
     */
    public static String getCameraModeContentDescription(int modeIndex,
            Context context) {
        String[] cameraModesDesc = context.getResources().getStringArray(
                                       R.array.camera_mode_content_description);
        if (modeIndex < 0 || modeIndex >= cameraModesDesc.length) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return new String();
        }
        return cameraModesDesc[modeIndex];
    }

    /**
     * Gets the shutter icon res id for a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return mode shutter icon id if the index is valid, otherwise 0.
     */
    public static int getCameraShutterIconId(int modeIndex, Context context) {
        // Find the camera mode icon using id
        TypedArray shutterIcons = context.getResources().obtainTypedArray(
                                      R.array.camera_mode_shutter_icon);
        if (modeIndex < 0 || modeIndex >= shutterIcons.length()) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            throw new IllegalStateException("Invalid mode index: " + modeIndex);
        }
        return shutterIcons.getResourceId(modeIndex, 0);
    }

    /**
     * Gets the parent mode that hosts a specific mode in nav drawer.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return mode id if the index is valid, otherwise 0
     */
    public static int getCameraModeParentModeId(int modeIndex, Context context) {
        // Find the camera mode icon using id
        int[] cameraModeParent = context.getResources().getIntArray(
                                     R.array.camera_mode_nested_in_nav_drawer);
        if (modeIndex < 0 || modeIndex >= cameraModeParent.length) {
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return cameraModeParent[modeIndex];
    }

    /**
     * Gets the mode cover icon resource id of a specific mode.
     *
     * @param modeIndex
     *            index of the mode
     * @param context
     *            current context
     * @return icon resource id if the index is valid, otherwise 0
     */
    public static int getCameraModeCoverIconResId(int modeIndex, Context context) {
        // Find the camera mode icon using id
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(
                                          R.array.camera_mode_cover_icon);
        Log.d(TAG, "----->>getCameraModeCoverIconResId length = " + cameraModesIcons.length());
        if (modeIndex >= cameraModesIcons.length() || modeIndex < 0) {
            // Mode index not found
            Log.e(TAG, "Invalid mode index: " + modeIndex);
            return 0;
        }
        return cameraModesIcons.getResourceId(modeIndex, 0);
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * <p>
     * Source: http://stackoverflow.com/questions/7962155/
     *
     * @return The number of cores, or 1 if failed to get result
     */
    public static int getNumCpuCores() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements java.io.FileFilter {
            @Override
            public boolean accept(java.io.File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (java.util.regex.Pattern.matches("cpu[0-9]+",
                                                    pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            // Get directory containing CPU info
            java.io.File dir = new java.io.File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            java.io.File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            // Default to return 1 core
            Log.e(TAG, "Failed to count number of cores, defaulting to 1", e);
            return 1;
        }
    }

    public static void showErrorAndFinish(final Activity activity, int msgId) {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };
        TypedValue out = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.alertDialogIcon,
                                             out, true);
        new AlertDialog.Builder(activity).setCancelable(false)
        .setTitle(R.string.camera_error_title).setMessage(msgId)
        .setNeutralButton(R.string.dialog_ok, buttonListener)
        .setIcon(out.resourceId).show();
    }


    /**
     * Clamps x to between min and max (inclusive on both ends, x = min --> min,
     * x = max --> max).
     */
    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * Clamps x to between min and max (inclusive on both ends, x = min --> min,
     * x = max --> max).
     */
    public static float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public static int getDisplayRotation(Context context) {
        WindowManager windowManager = (WindowManager) context
                                      .getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay()
                       .getRotation();
        switch (rotation) {
        case Surface.ROTATION_0:
            return 0;
        case Surface.ROTATION_90:
            return 90;
        case Surface.ROTATION_180:
            return 180;
        case Surface.ROTATION_270:
            return 270;
        }
        return 0;
    }

    public static int getCameraDisplayOrientation(Context context, int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = ((WindowManager) context
                        .getSystemService(Service.WINDOW_SERVICE)).getDefaultDisplay()
                       .getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static int getCameraJpegRoatation(CameraActivity activity,
            int mCameraId) {

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay()
                       .getRotation();
        int degrees = 0;
        switch (rotation) {
        case Surface.ROTATION_0:
            degrees = 0;
            break;
        case Surface.ROTATION_90:
            degrees = 90;
            break;
        case Surface.ROTATION_180:
            degrees = 180;
            break;
        case Surface.ROTATION_270:
            degrees = 270;
            break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        // See android.hardware.Camera.Parameters.setRotation for
        // documentation.
        int rotation = 0;
        if (orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            CameraInfo info = CameraHolder.instance().getCameraInfo()[cameraId];
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (info.orientation - orientation + 360) % 360;
            } else {  // back-facing camera
                rotation = (info.orientation + orientation) % 360;
            }
        }
        return rotation;
    }

    public static byte[] filterYuv420Data(byte[] fakeData, int width, int height) {
        int length = width * height;
        int targetLength = length * 3 / 2;
        if (targetLength == fakeData.length) {
            return fakeData;
        } else if (targetLength > fakeData.length) {
            // is not yuv420 format
            return null;
        }
        byte[] data = new byte[length * 3 / 2];
        // we assume width is already 64 aligned
        int alignHeight = getTopperNAlign(64, height);
        int alignWidth = getTopperNAlign(64, width);
        if(alignWidth > width) {
            for(int i = 0; i < height; i++) {
                System.arraycopy(fakeData, i*alignWidth, data, i*width, width);
            }
            int uvHeight = alignHeight + (int)(height * 0.5);
            int diffHeight = alignHeight - height;
            for(int i = alignHeight; i < uvHeight; i++) {
                System.arraycopy(fakeData, i*alignWidth, data, (i-diffHeight)*width, width);
            }
        } else {
            System.arraycopy(fakeData, 0, data, 0, length);
            System.arraycopy(fakeData, length + (alignHeight - height) * width,
                             data, length, length / 2);
        }
        fakeData = null;
        return data;
    }

    public static byte[] filterYuv420DataForQiku(byte[] fakeData, int width, int height) {
        int length = width * height;
        int targetLength = length * 3 / 2;
        if (targetLength == fakeData.length) {
            return fakeData;
        } else if (targetLength > fakeData.length) {
            // is not yuv420 format
            return null;
        }
        byte[] data = new byte[length * 3 / 2];
        // we assume width is already 64 aligned
        int alignHeight = 0;
        int alignWidth = 0;
        if(width > height) {
            alignWidth = 4160;
            alignHeight = 3136;
        } else {
            alignHeight = 4160;
            alignWidth = 3136;
        }
        if(alignWidth > width) {
            for(int i = 0; i < height; i++) {
                System.arraycopy(fakeData, i*alignWidth, data, i*width, width);
            }
            int uvHeight = alignHeight + (int)(height * 0.5);
            int diffHeight = alignHeight - height;
            for(int i = alignHeight; i < uvHeight; i++) {
                System.arraycopy(fakeData, i*alignWidth, data, (i-diffHeight)*width, width);
            }
        } else {
            System.arraycopy(fakeData, 0, data, 0, length);
            System.arraycopy(fakeData, length + (alignHeight - height) * width,
                             data, length, length / 2);
        }
        fakeData = null;
        return data;
    }

    // Orientation hysteresis amount used in rounding, in degrees
    private static final int ORIENTATION_HYSTERESIS = 5;
    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = ( dist >= 45 + ORIENTATION_HYSTERESIS );
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }

    /**
     * @param n
     *            must be 2^N
     * @param number
     *            >0
     * @return
     */
    public static int getTopperNAlign(int n, int number) {
        int tmp = number;
        int align = tmp & (n - 1);
        if (align != 0) {
            tmp = (tmp - align) + n;
        }
        return tmp;
    }

    public static String createJpegName(long dateTaken) {
        synchronized (sImageFileNamer) {
            return sImageFileNamer.generateName(dateTaken);
        }
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        // Keep compatibility
        context.sendBroadcast(new Intent("com.westalgo.factorycamera.NEW_PICTURE", uri));
    }

    private static class ImageFileNamer {
        private final SimpleDateFormat mFormat;

        // The date (in milliseconds) used to generate the last name.
        private long mLastDate;

        // Number of names generated for the same second.
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }
    }

    /**
     * This class is just a thread-safe queue for name,date holder objects.
     */
    public static class NamedImages {
        private final Vector<NamedEntity> mQueue;

        public NamedImages() {
            mQueue = new Vector<NamedEntity>();
        }

        public void nameNewImage(long date) {
            NamedEntity r = new NamedEntity();
            r.title = createJpegName(date);
            r.date = date;
            mQueue.add(r);
        }

        public NamedEntity getNextNameEntity() {
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    return mQueue.remove(0);
                }
            }
            return null;
        }

        public NamedEntity getCurrentEntity() {
            if(mQueue != null) {
                return mQueue.lastElement();
            }
            return null;
        }

        public static class NamedEntity {
            public String title;
            public long date;
        }
    }

    /**
     * whether in dual camera debug mode. If true, should save original photo of main and sub camera.
     * @param cmd the command to get the configuration, such as "getprop persist.camera.depth_debug"
     * @return true if debug mode is on, otherwise false.
     */
    public static boolean isInDualCamDebugMode(String cmd) {
        if (cmd == null || cmd == "") {
            return false;
        }
        Process proc = null;
        BufferedReader bis = null;
        try {
            proc = Runtime.getRuntime().exec(cmd);
            bis = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = bis.readLine();
            if (line != null && line != "") {
                int value = Integer.parseInt(line);
                return (value == 1);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
                proc.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void checkCameraFolder() {
        File saveDir = new File(Storage.DIRECTORY);
        if (!saveDir.exists()) {
            Log.d(TAG, "create folder: "+saveDir.toString());
            saveDir.mkdirs();
        }
    }

    public static void checkDebugFolder() {
        File saveDir = new File(Storage.DEBUG_DIRECTORY);
        if (!saveDir.exists()) {
            Log.d(TAG, "create folder: "+saveDir.toString());
            saveDir.mkdirs();
        }
    }

    public static void scanDebugFolder(Context context) {
        Uri mUri = Uri.fromFile(new File(Storage.DEBUG_DIRECTORY));
        // Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mUri);
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_MOUNTED, mUri);
        Log.d(TAG, "scanDebugFolder: " + mUri);
        context.sendBroadcast(scanIntent);
    }

    public static boolean isLandscape(CameraActivity activity) {
        int screenOrientation = activity.getResources().getConfiguration().orientation;
        if (screenOrientation == 2) {
            /**
             * ORIENTATION_PORTRAIT = 1
             * ORIENTATION_LANDSCAPE = 2
             */
            return true;
        }
        return false;
    }

    public static boolean isMTK() {
        String platform = Build.HARDWARE;
        if (platform.toUpperCase().contains(MTK_PLATFORM)) {
            return true;
        }else {
            return false;
        }
    }

}
