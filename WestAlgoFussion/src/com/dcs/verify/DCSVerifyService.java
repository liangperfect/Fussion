package com.dcs.verify;

import android.app.Service;
import android.net.Uri;
import android.os.IBinder;
import android.content.Intent;

import java.io.File;

import com.westalgo.factorycamera.debug.Log;

public class DCSVerifyService extends Service implements DCSVerifyEngineCallback {

    private static final Log.Tag TAG = new Log.Tag("DCSVerifyQueue");
    static DCSVerifyService globalCameraService = null;
    boolean serviceIsRunning = false;
    boolean isStop = false;

    DCSVerifyQueue sourceQueue = null;
    DCSVerifyItem runningItem = null;
    DCSVerifyCallBack callback = null;

    public static DCSVerifyService getInstance() {
        return globalCameraService;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "DCS Verify service onCreate");
        globalCameraService = this;

        File cacheDir = getExternalCacheDir();
        sourceQueue = new DCSVerifyQueue(cacheDir.getAbsolutePath());
        sourceQueue.initCacheLoad();
    }

    @Override
    public void onDestroy() {

        isStop = true;
        Log.i(TAG, "DCS Verify service onDestroy");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "DCS Verify service onStart");
        execute();

    }

    public void setVerifyCallBack(DCSVerifyCallBack callback0) {
        callback = callback0;
    }

    /**
     * execute history DCSDepthItem in thread pool.
     */
    public void execute() {
        if (!serviceIsRunning && !isStop) {
            Log.d(TAG, "###### Verify service execute history start ######");

            DCSVerifyItem item = sourceQueue.dequeue();
            if (item != null) {
                serviceIsRunning = true;
                runningItem = item;
                DCSVerifyEngine.getEngine().asyncDoVerify(item, true, this);
            }
            Log.d(TAG, "###### Verify service execute history end ######");
        }
    }

    /**
     * execute this item in thread pool.
     *
     * @param item The item to execute
     */
    public void execute(DCSVerifyItem item) {
        if (!serviceIsRunning) {
            Log.d(TAG, "Verify service execute  service start running");
            serviceIsRunning = true;
            runningItem = item;
            DCSVerifyEngine.getEngine().asyncDoVerify(item, false, this);
        } else {
            Log.d(TAG, "Verify service execute  service is already running , enqueue the latest item");
            sourceQueue.enqueue(item);
        }
    }


    /**
     * To call back (@depthGenerateFinished) when verify success.
     */
    public interface DCSVerifyCallBack {
//        void depthGenerateFinished(String filePath);
        void verifyFinished(float verifyinfo[]);
    }

    @Override
//    public void doVerifyFinish(String tag, String finalFilePath) {
    public void doVerifyFinish(String tag){
        // TODO Auto-generated method stub
        Log.d(TAG, "VerifyCreateFinish tag = " + tag + "  running tag = " + runningItem.tag);
        serviceIsRunning = false;
        if (runningItem.tag.equals(tag)) {
            sourceQueue.removeDiskDepthItem(runningItem);
        }

        if (callback != null) {
//            callback.depthGenerateFinished(runningItem.mDepthSavePath);
            callback.verifyFinished(runningItem.mVerifyInfo);
        }
//        fileScan(runningItem.mDepthSavePath);
        runningItem = null;
        if (!isStop) {
            execute();
        }
    }

    @Override
    public void doVerifyError(String tag) {
        serviceIsRunning = false;
        if (tag != null && runningItem.tag.equals(tag)) {
            sourceQueue.removeDiskDepthItem(runningItem);
        }
        runningItem = null;
        if (!isStop) {
            execute();
        }
    }
}
