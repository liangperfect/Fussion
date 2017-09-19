package com.westalgo.factorycamera.manager;

import java.io.IOException;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import java.lang.reflect.Method;

import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.settings.ProjectConfig;

public class AndroidCameraManagerImpl implements CameraManager {
    private static final Log.Tag TAG = new Log.Tag("AndroidCameraManager");
    private static final int CAMERA_HAL_API_VERSION_1_0 = 0x100;
    private ConditionVariable mSig = new ConditionVariable();

    /* Messages used in CameraHandler. */
    // Camera initialization/finalization
    private static final int OPEN_CAMERA = 1;
    private static final int RELEASE = 2;
    private static final int RECONNECT = 3;
    // Preview
    private static final int SET_PREVIEW_TEXTURE_ASYNC = 101;
    private static final int START_PREVIEW_ASYNC = 102;
    private static final int STOP_PREVIEW = 103;
    private static final int SET_PREVIEW_DISPLAY_ASYNC = 104;
    // Parameters
    private static final int SET_PARAMETERS = 201;
    private static final int GET_PARAMETERS = 202;
    private static final int REFRESH_PARAMETERS = 203;
    // Focu
    private static final int AUTO_FOCUS = 301;
    private static final int CANCEL_AUTO_FOCUS = 302;
    private static final int SET_AUTO_FOCUS_MOVE_CALLBACK = 303;

    private static final int SET_ERROR_CALLBACK = 464;
    // Presentation
    private static final int ENABLE_SHUTTER_SOUND = 501;
    private static final int SET_DISPLAY_ORIENTATION = 502;

    private CameraHandler mCameraHandler;
    private android.hardware.Camera mCamera;

    private Parameters mParameters;
    private boolean mParametersIsDirty;
    private IOException mReconnectIOException;

    public AndroidCameraManagerImpl() {
        HandlerThread handlerThread = new HandlerThread("CameraManager Thread");
        handlerThread.start();
        mCameraHandler = new CameraHandler(handlerThread.getLooper());
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            try {
                switch (msg.what) {
                case OPEN_CAMERA:
                    try {
                        mCamera = openCamera(msg.arg1);
                    } catch (RuntimeException e) {
                        mCamera = tryOpenTwice(msg.arg1);
                    }
                    if (mCamera != null) {
                        mParametersIsDirty = true;
                    } else {
                        if (msg.obj != null) {
                            ((CameraOpenErrorCallback) msg.obj)
                            .onDeviceOpenFailure(msg.arg1);
                        }
                    }
                    return;
                case RELEASE:
                    if (mCamera == null)
                        return;
                    mCamera.release();
                    mCamera = null;
                    return;
                case RECONNECT:
                    mReconnectIOException = null;
                    try {
                        mCamera.reconnect();
                    } catch (IOException ex) {
                        mReconnectIOException = ex;
                    }
                    return;
                case SET_PREVIEW_TEXTURE_ASYNC:
                    try {
                        mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                    } catch (IOException e) {
                        Log.e(TAG, "Could not set preview texture", e);
                    }
                    return;
                case SET_PREVIEW_DISPLAY_ASYNC:
                    try {
                        mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                case START_PREVIEW_ASYNC:
                    mCamera.startPreview();
                    return;

                case STOP_PREVIEW:
                    mCamera.stopPreview();
                    return;

                case AUTO_FOCUS:
                    mCamera.autoFocus((AutoFocusCallback) msg.obj);
                    return;

                case CANCEL_AUTO_FOCUS:
                    mCamera.cancelAutoFocus();
                    return;

                case SET_AUTO_FOCUS_MOVE_CALLBACK:
                    setAutoFocusMoveCallback(mCamera, msg.obj);
                    return;

                case SET_DISPLAY_ORIENTATION:
                    mCamera.setDisplayOrientation(msg.arg1);
                    return;
                case SET_ERROR_CALLBACK:
                    mCamera.setErrorCallback((ErrorCallback) msg.obj);
                    return;

                case SET_PARAMETERS:
                    mParametersIsDirty = true;
                    mCamera.setParameters((Parameters) msg.obj);
                    mSig.open();
                    break;

                case GET_PARAMETERS:
                    if (mParametersIsDirty) {
                        mParameters = mCamera.getParameters();
                        mParametersIsDirty = false;
                    }
                    return;
                case ENABLE_SHUTTER_SOUND:
                    enableShutterSound((msg.arg1 == 1) ? true : false);
                    return;

                case REFRESH_PARAMETERS:
                    mParametersIsDirty = true;
                    return;
                default:
                    throw new RuntimeException("Invalid CameraProxy message="
                                               + msg.what);
                }
            } catch (RuntimeException e) {

            }
        }

        private Camera openCamera(int cameraId) {
            // TODO Auto-generated method stub
            android.hardware.Camera camera = null;
            try {
                if (ProjectConfig.useHal1) {
                    Method openMethod = Class.forName("android.hardware.Camera").getMethod(
                            "openLegacy", int.class, int.class);
                    camera = (android.hardware.Camera) openMethod.invoke(
                            null, cameraId, CAMERA_HAL_API_VERSION_1_0);
                } else {
                    camera = android.hardware.Camera.open(cameraId);
                }

            } catch (Exception e) {
                Log.e(TAG, "openCamera Camera "+ cameraId + " open error.", e);
                camera = android.hardware.Camera.open(cameraId);
            }
            return camera;
        }

        private android.hardware.Camera tryOpenTwice(int cameraId) {
            android.hardware.Camera camera = null;
            for (int i = 0; i < 2 && mCamera == null; i++) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e(TAG, "tryOpenTwice failed.", e);
                }
                try {
                    camera = openCamera(cameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return camera;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        private void enableShutterSound(boolean enable) {
            mCamera.enableShutterSound(enable);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private void setAutoFocusMoveCallback(android.hardware.Camera camera,
                                              Object cb) {
            camera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
        }

        public void requestTakePicture(final ShutterCallback shutter,
                                       final PictureCallback raw, final PictureCallback postView,
                                       final PictureCallback jpeg) {
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCamera.takePicture(shutter, raw, postView, jpeg);
                    } catch (RuntimeException e) {
                        // TODO: output camera state and focus state for
                        // debugging.
                        Log.e(TAG, "take picture failed.");
                        throw e;
                    }
                }
            });
        }

        /**
         * Waits for all the {@code Message} and {@code Runnable} currently in
         * the queue are processed.
         *
         * @return {@code false} if the wait was interrupted, {@code true}
         *         otherwise.
         */
        public boolean waitDone() {
            final Object waitDoneLock = new Object();
            final Runnable unlockRunnable = new Runnable() {
                @Override
                public void run() {
                    synchronized (waitDoneLock) {
                        waitDoneLock.notifyAll();
                    }
                }
            };

            synchronized (waitDoneLock) {
                mCameraHandler.post(unlockRunnable);
                try {
                    waitDoneLock.wait();
                } catch (InterruptedException ex) {
                    Log.v(TAG, "waitDone interrupted");
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A class which implements {@link CameraManager.CameraProxy} and camera
     * handler thread. TODO: Save the handler for the callback here to avoid
     * passing the same handler multiple times.
     */
    public class AndroidCameraProxyImpl implements CameraManager.CameraProxy {

        private int mCameraId = -1;
        public AndroidCameraProxyImpl(int cameraId) {
            if (mCamera == null) {
                throw new Error("No Camera Device is opened!");
            }
            mCameraId = cameraId;
        }

        @Override
        public int getCameraId() {
            return mCameraId;
        }

        @Override
        public Camera getCamera() {
            // TODO Auto-generated method stub
            return mCamera;
        }

        @Override
        public void release() {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(RELEASE);
            mCameraHandler.waitDone();
        }

        @Override
        public boolean reconnect(Handler handler, CameraOpenErrorCallback cb) {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(RECONNECT);
            mCameraHandler.waitDone();
            if (mReconnectIOException != null) {
                cb.onReconnectionFailure(AndroidCameraManagerImpl.this);
                return false;
            }
            return true;
        }

        @Override
        public void setPreviewTexture(SurfaceTexture surfaceTexture) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(SET_PREVIEW_TEXTURE_ASYNC,
                                         surfaceTexture).sendToTarget();
        }

        @Override
        public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(SET_PREVIEW_DISPLAY_ASYNC,
                                         surfaceHolder).sendToTarget();

        }

        @Override
        public void startPreview() {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(START_PREVIEW_ASYNC);

        }

        @Override
        public void stopPreview() {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(STOP_PREVIEW);
            mCameraHandler.waitDone();

        }

        @Override
        public void autoFocus(Handler handler, CameraAFCallback cb) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(AUTO_FOCUS,
                                         AFCallbackForward.getNewInstance(handler, this, cb))
            .sendToTarget();
        }

        @Override
        public void cancelAutoFocus() {
            // TODO Auto-generated method stub
            mCameraHandler.removeMessages(AUTO_FOCUS);
            mCameraHandler.sendEmptyMessage(CANCEL_AUTO_FOCUS);

        }

        @Override
        public void setAutoFocusMoveCallback(Handler handler,
                                             CameraAFMoveCallback cb) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(SET_AUTO_FOCUS_MOVE_CALLBACK,
                                         AFMoveCallbackForward.getNewInstance(handler, this, cb))
            .sendToTarget();

        }

        @Override
        public void takePicture(Handler handler, CameraShutterCallback shutter,
                                CameraPictureCallback raw, CameraPictureCallback post,
                                CameraPictureCallback jpeg) {
            // TODO Auto-generated method stub
            mCameraHandler.requestTakePicture(ShutterCallbackForward
                                              .getNewInstance(handler, this, shutter),
                                              PictureCallbackForward.getNewInstance(handler, this, raw),
                                              PictureCallbackForward.getNewInstance(handler, this, post),
                                              PictureCallbackForward.getNewInstance(handler, this, jpeg));

        }

        @Override
        public void setDisplayOrientation(int degrees) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, degrees, 0)
            .sendToTarget();

        }

        @Override
        public void setErrorCallback(ErrorCallback cb) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(SET_ERROR_CALLBACK, cb).sendToTarget();
        }

        @Override
        public void setParameters(Parameters params) {
            // TODO Auto-generated method stub
            mSig.close();
            mCameraHandler.obtainMessage(SET_PARAMETERS, params).sendToTarget();
            mSig.block();

        }

        @Override
        public Parameters getParameters() {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(GET_PARAMETERS);
            mCameraHandler.waitDone();
            return mParameters;
        }

        @Override
        public void refreshParameters() {
            // TODO Auto-generated method stub
            mCameraHandler.sendEmptyMessage(REFRESH_PARAMETERS);
        }

        @Override
        public void enableShutterSound(boolean enable) {
            // TODO Auto-generated method stub
            mCameraHandler.obtainMessage(ENABLE_SHUTTER_SOUND,
                                         (enable ? 1 : 0), 0).sendToTarget();
        }
    }

    @Override
    public CameraProxy cameraOpen(Handler handler, int cameraId,
                                  CameraOpenErrorCallback callback) {
        // TODO Auto-generated method stub
        mCameraHandler.obtainMessage(OPEN_CAMERA, cameraId, 0,
                                     CameraOpenErrorCallbackForward.getNewInstance(
                                         handler, callback)).sendToTarget();
        mCameraHandler.waitDone();
        if (mCamera != null) {
            return new AndroidCameraProxyImpl(cameraId);
        } else {
            return null;
        }
    }

    /**
     * A callback helps to invoke the original callback on another
     * {@link android.os.Handler}.
     *
     * mHandler is main Handler
     */
    private static class CameraOpenErrorCallbackForward implements
        CameraOpenErrorCallback {
        private final Handler mHandler;
        private final CameraOpenErrorCallback mCallback;

        /**
         * Returns a new instance of {@link FaceDetectionCallbackForward}.
         *
         * @param handler
         *            The handler in which the callback will be invoked in.
         * @param cb
         *            The callback to be invoked.
         * @return The instance of the {@link FaceDetectionCallbackForward}, or
         *         null if any parameter is null.
         */
        public static CameraOpenErrorCallbackForward getNewInstance(
            Handler handler, CameraOpenErrorCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenErrorCallbackForward(handler, cb);
        }

        private CameraOpenErrorCallbackForward(Handler h,
                                               CameraOpenErrorCallback cb) {
            // Given that we are using the main thread handler, we can create it
            // here instead of holding onto the PhotoModule objects. In this
            // way, we can avoid memory leak.
            mHandler = new Handler(Looper.getMainLooper());
            mCallback = cb;
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            // TODO Auto-generated method stub
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mCallback.onCameraDisabled(cameraId);
                }
            });
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId) {
            // TODO Auto-generated method stub
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mCallback.onDeviceOpenFailure(cameraId);
                }

            });
        }

        @Override
        public void onReconnectionFailure(final CameraManager mgr) {
            // TODO Auto-generated method stub
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    mCallback.onReconnectionFailure(mgr);
                }

            });
        }
    }

    /**
     * A helper class to forward AutoFocusCallback to another thread.
     */
    private static class AFCallbackForward implements AutoFocusCallback {
        private final Handler mHandler;
        private final CameraProxy mCamera;
        private final CameraAFCallback mCallback;

        /**
         * Returns a new instance of {@link AFCallbackForward}.
         *
         * @param handler
         *            The handler in which the callback will be invoked in.
         * @param camera
         *            The {@link CameraProxy} which the callback is from.
         * @param cb
         *            The callback to be invoked.
         * @return The instance of the {@link AFCallbackForward}, or null if any
         *         parameter is null.
         */
        public static AFCallbackForward getNewInstance(Handler handler,
                CameraProxy camera, CameraAFCallback cb) {
            if (handler == null || camera == null || cb == null)
                return null;
            return new AFCallbackForward(handler, camera, cb);
        }

        private AFCallbackForward(Handler h, CameraProxy camera,
                                  CameraAFCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onAutoFocus(final boolean b, Camera camera) {
            final android.hardware.Camera currentCamera = mCamera.getCamera();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentCamera.equals(mCamera.getCamera())) {
                        mCallback.onAutoFocus(b, mCamera);
                    }
                }
            });
        }
    }

    /** A helper class to forward AutoFocusMoveCallback to another thread. */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static class AFMoveCallbackForward implements AutoFocusMoveCallback {
        private final Handler mHandler;
        private final CameraAFMoveCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link AFMoveCallbackForward}.
         *
         * @param handler
         *            The handler in which the callback will be invoked in.
         * @param camera
         *            The {@link CameraProxy} which the callback is from.
         * @param cb
         *            The callback to be invoked.
         * @return The instance of the {@link AFMoveCallbackForward}, or null if
         *         any parameter is null.
         */
        public static AFMoveCallbackForward getNewInstance(Handler handler,
                CameraProxy camera, CameraAFMoveCallback cb) {
            if (handler == null || camera == null || cb == null)
                return null;
            return new AFMoveCallbackForward(handler, camera, cb);
        }

        private AFMoveCallbackForward(Handler h, CameraProxy camera,
                                      CameraAFMoveCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onAutoFocusMoving(final boolean moving,
                                      android.hardware.Camera camera) {
            final android.hardware.Camera currentCamera = mCamera.getCamera();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentCamera.equals(mCamera.getCamera())) {
//                        Log.e(TAG,"-----------onAutoFocusMoving id:"+ mCamera.getCameraId());
                        mCallback.onAutoFocusMoving(moving, mCamera);
                    }
                }
            });
        }
    }

    /**
     * A helper class to forward ShutterCallback to to another thread.
     */
    private static class ShutterCallbackForward implements ShutterCallback {
        private final Handler mHandler;
        private final CameraShutterCallback mCallback;
        private final CameraProxy mCamera;

        // private final boolean mDirectCallback;

        /**
         * Returns a new instance of {@link ShutterCallbackForward}.
         *
         * @param handler
         *            The handler in which the callback will be invoked in.
         * @param camera
         *            The {@link CameraProxy} which the callback is from.
         * @param cb
         *            The callback to be invoked.
         * @return The instance of the {@link ShutterCallbackForward}, or null
         *         if any parameter is null.
         */
        public static ShutterCallbackForward getNewInstance(Handler handler,
                CameraProxy camera, CameraShutterCallback cb) {
            if (handler == null || camera == null || cb == null)
                return null;
            return new ShutterCallbackForward(handler, camera, cb);
        }

        private ShutterCallbackForward(Handler h, CameraProxy camera,
                                       CameraShutterCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onShutter() {
            final android.hardware.Camera currentCamera = mCamera.getCamera();
            // if (mDirectCallback) {
            // mCallback.onShutter(mCamera);
            // } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentCamera.equals(mCamera.getCamera())) {
                        mCallback.onShutter(mCamera);
                    }
                }
            });
            // }
        }
    }

    /**
     * A helper class to forward PictureCallback to another thread.
     */
    private static class PictureCallbackForward implements PictureCallback {
        private final Handler mHandler;
        private final CameraPictureCallback mCallback;
        private final CameraProxy mCamera;

        /**
         * Returns a new instance of {@link PictureCallbackForward}.
         *
         * @param handler
         *            The handler in which the callback will be invoked in.
         * @param camera
         *            The {@link CameraProxy} which the callback is from.
         * @param cb
         *            The callback to be invoked.
         * @return The instance of the {@link PictureCallbackForward}, or null
         *         if any parameters is null.
         */
        public static PictureCallbackForward getNewInstance(Handler handler,
                CameraProxy camera, CameraPictureCallback cb) {
            if (handler == null || camera == null || cb == null)
                return null;
            return new PictureCallbackForward(handler, camera, cb);
        }

        private PictureCallbackForward(Handler h, CameraProxy camera,
                                       CameraPictureCallback cb) {
            mHandler = h;
            mCamera = camera;
            mCallback = cb;
        }

        @Override
        public void onPictureTaken(final byte[] data,
                                   android.hardware.Camera camera) {
            final android.hardware.Camera currentCamera = mCamera.getCamera();

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (currentCamera.equals(mCamera.getCamera())) {
                        mCallback.onPictureTaken(data, mCamera);
                    }
                }
            });
        }
    }

}
