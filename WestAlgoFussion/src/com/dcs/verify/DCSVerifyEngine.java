package com.dcs.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.westalgo.factorycamera.debug.Log;
import com.dcs.verify.DCSUtils;
import com.dcs.verify.DCSVerify;


public class DCSVerifyEngine {
    private static final Log.Tag TAG = new Log.Tag("DCSVerifyEngine");

    private static String PREFIX_PATH = Environment.getExternalStorageDirectory().toString() + "/DCIM/";
    Handler mMainHandler = new Handler(Looper.getMainLooper());

    static DCSVerifyEngine mEngine = new DCSVerifyEngine();

    public static DCSVerifyEngine getEngine() {

        return mEngine;
    }

    public static Bitmap yuv2Bitmap(int width, int height, byte[] leftData) {

        Bitmap bit = null;
        YuvImage limage = new YuvImage(leftData, ImageFormat.NV21, width,
                                       height, null);

        if (limage != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {

                limage.compressToJpeg(new Rect(0, 0, width, height), 95, stream);

            } catch (Exception e) {
            }

            ByteArrayInputStream is = new ByteArrayInputStream(leftData);

            bit = BitmapFactory.decodeStream(is);

            try {
                stream.close();
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        return bit;
    }

    /**
     * Do verify in synchronize way.
     *
     * @param item The depth item
     * @param needLoad If the item need load again
     * @param callback Call depthCreateFinish method if depth generate finished
     */
    public void asyncDoVerify(final DCSVerifyItem item,final  boolean needLoad,final DCSVerifyEngineCallback callback) {
        DCSThreadManager.instance().add(new Runnable() {
            @Override
            public void run() {

                ////////////////////////////////////////////////////////////////////////////////
                // check item validity
                ////////////////////////////////////////////////////////////////////////////////
                if(needLoad) {
                    if (item.empty()) {
                        item.initLoad();
                    }

                    if (!item.isValid()) {
                        mMainHandler.post(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                if (callback != null) {
                                    callback.doVerifyError(item.tag);
                                }
                            }
                        });
                        return;
                    }
                }

                ///////////////////////////////////////////////////////////////////////////////////////////////
                // main camera
                Bitmap mainBitmap = BitmapFactory.decodeByteArray(item.mMainImgData, 0,item.mMainImgData.length);
                Mat mainMat = DCSUtils.bitmap2Mat(mainBitmap);
                Imgproc.cvtColor(mainMat, mainMat, Imgproc.COLOR_RGBA2BGR);
                if(mainBitmap != null){
                    mainBitmap.recycle();
                    mainBitmap = null;
                }

                Mat subMat = new Mat();
                switch (item.mSubPictureFormat) {
                    case ImageFormat.JPEG:
                        // sub camera JPEG image format
                        Bitmap subBitmap = BitmapFactory.decodeByteArray(item.mSubImgData, 0,item.mSubImgData.length);
                        subMat = DCSUtils.bitmap2Mat(subBitmap);
                        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGBA2BGR);
                        if(subBitmap != null){
                            subBitmap.recycle();
                            subBitmap = null;
                        }
                        break;
                    case ImageFormat.NV21:
                        // sub camera NV21 image format
                        Mat subYUV = new Mat((int) (1.5 * item.mSubImgH), item.mSubImgW, CvType.CV_8U);
                        subYUV.put(0, 0, item.mSubImgData);
                        subMat = new Mat(item.mSubImgH, item.mSubImgW, CvType.CV_8U);
                        Imgproc.cvtColor(subYUV, subMat, Imgproc.COLOR_YUV2BGR_NV21);
                        if(!subYUV.empty()) {
                            subYUV.release();
                        }
                        break;
                    default:
                        Log.e(TAG, "-->>onPictureTaken: Unknow SubCamera Image Format");
                }

                //the method for verify
                Log.d(TAG, "initVerify...");
                if (DCSVerify.initVerify(mainMat.cols(), mainMat.rows(), subMat.cols(), subMat.rows())) {
                    Log.d(TAG, "startVerify...");
                    int result = DCSVerify.doVerify(mainMat.nativeObj, item.mMainOrientation, subMat.nativeObj, item.mSubOrientation, item.correctionMode);
                    Log.i(TAG, "the verify result: " + result);

                    float resultInfo[] = new float[8];
                    resultInfo[0] = result;
                    resultInfo[1] = DCSVerify.getErr();
                    resultInfo[2] = DCSVerify.getDistance();
                    resultInfo[3] = DCSVerify.getDeltaY();
                    resultInfo[4] = DCSVerify.getOriL();
                    resultInfo[5] = DCSVerify.getDstL();
                    resultInfo[6] = DCSVerify.getOriR();
                    resultInfo[7] = DCSVerify.getDstR();
                    item.mVerifyInfo = resultInfo;
                } else {
                    Log.e(TAG, "init Verify failed!");
                }


                if(!mainMat.empty()){
                    mainMat.release();
                }

                if(!subMat.empty()){
                    subMat.release();
                }

                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.doVerifyFinish(item.tag);
                        }
                    }
                });
            }
        });
    }

    public byte[] compressToBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }
}
