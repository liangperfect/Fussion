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

package com.westalgo.factorycamera.manager;

import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.manager.CameraManager.CameraProxy;
import com.westalgo.factorycamera.settings.ProjectConfig;

/**
 * The class is used to hold an {@code android.hardware.Camera} instance.
 *
 * <p>
 * The {@code open()} and {@code release()} calls are similar to the ones in
 * {@code android.hardware.Camera}. The difference is if {@code keep()} is
 * called before {@code release()}, CameraHolder will try to hold the
 * {@code android.hardware.Camera} instance for a while, so if {@code open()} is
 * called soon after, we can avoid the cost of {@code open()} in
 * {@code android.hardware.Camera}.
 *
 * <p>
 * This is used in switching between different modules.
 */
public class CameraHolder {
    private static final Log.Tag TAG = new Log.Tag("CameraHolder");
    public static final int CAMERA_MAIN_ID = ProjectConfig.getConfig().getMainId();
    public static final int CAMERA_SUB_ID = ProjectConfig.getConfig().getAuxId();
    public static  int SINGLE_CAMERA_ID = 0;
    public static final int CAMERA_FRONT_ID = -1;

    //Message
    private static final int MSG_RELEASE_MAIN_CAMERA = 1;
    private static final int MSG_RELEASE_SUB_CAMERA = 2;
    private static final int KEEP_CAMERA_TIMEOUT = 3000; // 3 seconds

    // Main or Front Camera
    private CameraProxy mMainCameraDevice;
    private boolean mMainCameraOpened; // true if camera is opened
    private int mMainCameraId = -1; // current camera id
    // We store the camera parameters when we actually open the device,
    // so we can restore them in the subsequent open() requests by the user.
    private Parameters mMainParameters;
    // Sub Camera Device
    private CameraProxy mSubCameraDevice;
    private boolean mSubCameraOpened; // true if camera is opened
    private int mSubCameraId = -1; // current camera id
    private Parameters mSubParameters;

    private long mKeepBeforeTime; // Keep the Camera before this time.
    private final Handler mHandler;
    private final int mNumberOfCameras;
    private final CameraInfo[] mInfo;


    // Use a singleton.
    private static CameraHolder sHolder;

    public static synchronized CameraHolder instance() {
        if (sHolder == null) {
            sHolder = new CameraHolder();
        }
        return sHolder;
    }

    private class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_RELEASE_MAIN_CAMERA:
                synchronized (CameraHolder.this) {
                    // In 'CameraHolder.open', the 'MSG_RELEASE_MAIN_CAMERA' message
                    // will be removed if it is found in the queue. However,
                    // there is a chance that this message has been handled
                    // before being removed. So, we need to add a check
                    // here:
                    if (!mMainCameraOpened)
                        releaseMain();
                }
            case MSG_RELEASE_SUB_CAMERA:
                synchronized (CameraHolder.this) {
                    if (!mSubCameraOpened)
                        releaseSub();
                }
                break;
            }
        }
    }

    private CameraHolder() {
        HandlerThread ht = new HandlerThread("CameraHolder");
        ht.start();
        mHandler = new MyHandler(ht.getLooper());

        mNumberOfCameras = android.hardware.Camera.getNumberOfCameras();
        mInfo = new CameraInfo[mNumberOfCameras];
        for (int i = 0; i < mNumberOfCameras; i++) {
            mInfo[i] = new CameraInfo();
            android.hardware.Camera.getCameraInfo(i, mInfo[i]);
        }
    }

    public int getNumberOfCameras() {
        return mNumberOfCameras;
    }

    public void setNextSingleCameraId() {
        SINGLE_CAMERA_ID ++;
        if (SINGLE_CAMERA_ID >= mNumberOfCameras) {
            SINGLE_CAMERA_ID = 0;
        }
    }

    public CameraInfo[] getCameraInfo() {
        return mInfo;
    }

    public synchronized CameraProxy openMainCamera(Handler handler, int cameraId,
            CameraManager.CameraOpenErrorCallback cb) {
        if (mMainCameraOpened) {
            throw new Error("Camera :"+ cameraId + " is opened!");
        }
        if (mMainCameraDevice != null && mMainCameraId != cameraId) {
            mMainCameraDevice.release();
            mMainCameraDevice = null;
            mMainCameraId = -1;
        }
        if (mMainCameraDevice == null) {
            Log.v(TAG, "open camera " + cameraId);
            mMainCameraDevice = CameraManagerFactory.getAndroidCameraManager()
                                .cameraOpen(handler, cameraId, cb);

            mMainCameraId = cameraId;
            mMainParameters = mMainCameraDevice.getCamera().getParameters();
        } else {
            if (!mMainCameraDevice.reconnect(handler, cb)) {
                Log.e(TAG, "fail to reconnect Camera:" + mMainCameraId
                      + ", aborting.");
                return null;
            }
        }
        mMainCameraDevice.setParameters(mMainParameters);
        mMainCameraOpened = true;
        mHandler.removeMessages(MSG_RELEASE_MAIN_CAMERA);
        mKeepBeforeTime = 0;
        return mMainCameraDevice;
    }

    public synchronized void releaseMain() {

        if (mMainCameraDevice == null)
            return;

        long now = System.currentTimeMillis();
        if (now < mKeepBeforeTime) {
            if (mMainCameraOpened) {
                mMainCameraOpened = false;
                mMainCameraDevice.stopPreview();
            }
            mHandler.sendEmptyMessageDelayed(MSG_RELEASE_MAIN_CAMERA, mKeepBeforeTime - now);
            return;
        }
        strongReleaseMain();
    }

    public synchronized void strongReleaseMain() {
        if (mMainCameraDevice == null)
            return;

        mMainCameraOpened = false;
        mMainCameraDevice.release();
        mMainCameraDevice = null;
        // We must set this to null because it has a reference to Camera.
        // Camera has references to the listeners.
        mMainParameters = null;
        mMainCameraId = -1;
    }

    public synchronized CameraProxy openSubCamera(Handler handler, int cameraId,
            CameraManager.CameraOpenErrorCallback cb) {
        if (mSubCameraOpened) {
            throw new Error("Camera :"+ cameraId + " is opened!");
        }
        if (mSubCameraDevice != null && mSubCameraId != cameraId) {
            mSubCameraDevice.release();
            mSubCameraDevice = null;
            mSubCameraId = -1;
        }
        if (mSubCameraDevice == null) {
            Log.v(TAG, "open camera " + cameraId);
            mSubCameraDevice = CameraManagerFactory.getAndroidCameraManager2()
                               .cameraOpen(handler, cameraId, cb);

            mSubCameraId = cameraId;
            mSubParameters = mSubCameraDevice.getCamera().getParameters();
        } else {
            if (!mSubCameraDevice.reconnect(handler, cb)) {
                Log.e(TAG, "fail to reconnect Camera:" + mSubCameraId
                      + ", aborting.");
                return null;
            }
        }
        mSubCameraDevice.setParameters(mSubParameters);
        mSubCameraOpened = true;
        mHandler.removeMessages(MSG_RELEASE_SUB_CAMERA);
        mKeepBeforeTime = 0;
        return mSubCameraDevice;
    }

    public synchronized void releaseSub() {

        if (mSubCameraDevice == null)
            return;

        long now = System.currentTimeMillis();
        if (now < mKeepBeforeTime) {
            if (mSubCameraOpened) {
                mSubCameraOpened = false;
                mSubCameraDevice.stopPreview();
            }
            mHandler.sendEmptyMessageDelayed(MSG_RELEASE_SUB_CAMERA, mKeepBeforeTime - now);
            return;
        }
        strongReleaseSub();
    }

    public synchronized void strongReleaseSub() {
        if (mSubCameraDevice == null)
            return;

        mSubCameraOpened = false;
        mSubCameraDevice.release();
        mSubCameraDevice = null;
        mSubParameters = null;
        mSubCameraId = -1;
    }

    public void keep() {
        keep(KEEP_CAMERA_TIMEOUT);
    }

    public synchronized void keep(int time) {
        // We allow mMainCameraOpened/mSubCameraOpened in either state for the
        // convenience of the
        // calling activity. The activity may not have a chance to call open()
        // before the user switches to another activity.
        mKeepBeforeTime = System.currentTimeMillis() + time;
    }

}
