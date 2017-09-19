package com.westalgo.factorycamera.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dcs.verify.DCSVerifyItem;
import com.dcs.verify.DCSUtils;
import com.dcs.verify.DCSVerify;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by wenzhe on 4/13/17.
 */

public class VerifyHelper {
    private final String TAG = this.getClass().getSimpleName();

    public interface Callback{
        void onVerifyFinished(int result,float[] info);
    }

    private Callback mCallback;

    private static VerifyHelper verifyHelper;
    private Handler mHander;

    private VerifyHelper() {
        mHander = new Handler(Looper.getMainLooper());
    }

    public static VerifyHelper instance() {
        if (verifyHelper == null) {
            verifyHelper = new VerifyHelper();
        }
        return verifyHelper;
    }

    public synchronized void doVerify(DCSVerifyItem item, Callback callback) {
        mCallback = callback;
        new VerifyThread(item).start();
    }

    private Mat getMatFromByteArray(byte[] bytes, int format, int w, int h) {
        Mat resultMat = null;
        switch (format) {
            case ImageFormat.JPEG:{
                Bitmap mainBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                resultMat = DCSUtils.bitmap2Mat(mainBitmap);
                Imgproc.cvtColor(resultMat, resultMat, Imgproc.COLOR_RGBA2BGR);
                if (mainBitmap != null) {
                    mainBitmap.recycle();
                }
            }
                break;
            case ImageFormat.NV21:{
                Mat subYUV = new Mat((int) (1.5 * h), w, CvType.CV_8U);
                subYUV.put(0, 0, bytes);
                resultMat = new Mat(h, w, CvType.CV_8U);
                Imgproc.cvtColor(subYUV, resultMat, Imgproc.COLOR_YUV2BGR_NV21);
                if (!subYUV.empty()) {
                    subYUV.release();
                }
            }
                break;
            default:
                break;
        }
        return resultMat;
    }

    private class VerifyThread extends Thread {
        private DCSVerifyItem mItem;
        public VerifyThread(DCSVerifyItem item) {
            mItem = item;
        }
        public void run() {
            super.run();
            Mat mainMat = getMatFromByteArray(
                    mItem.mMainImgData, ImageFormat.JPEG, mItem.mMainImgW, mItem.mMainImgH);
            Mat auxMat = getMatFromByteArray(
                    mItem.mSubImgData, mItem.mSubPictureFormat, mItem.mSubImgW, mItem.mSubImgH);
            Log.d(TAG, "start verify");
            if (DCSVerify.initVerify(
                    mainMat.cols(), mainMat.rows(), auxMat.cols(), auxMat.rows())) {
                final int result = DCSVerify.doVerify(mainMat.nativeObj, mItem.mMainOrientation,
                        auxMat.nativeObj, mItem.mSubOrientation, mItem.correctionMode);
                Log.d(TAG, " verify result code:" + result);
                final float resultInfo[] = new float[]{
                    DCSVerify.getErr(),
                    DCSVerify.getDistance(),
                    DCSVerify.getDeltaY(),
                    DCSVerify.getOriL(),
                    DCSVerify.getDstL(),
                    DCSVerify.getOriR(),
                    DCSVerify.getDstR()
                };
                mHander.post(new Runnable() {
                    public void run() {
                        mCallback.onVerifyFinished(result, resultInfo);
                    }
                });
            } else {
                Log.e(TAG, "init Verify failed!");
            }
            //release
            DCSVerify.endVerify();
            if(!mainMat.empty()){
                mainMat.release();
            }
            if(!auxMat.empty()){
                auxMat.release();
            }
        }
    }


}

