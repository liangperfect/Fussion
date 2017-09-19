package com.dcs.verify;

import android.content.Intent;
import android.util.Log;

import com.dcs.verify.DCSVerify;
import com.dcs.verify.DCSVerifyService;
import com.dcs.verify.DCSVerifyService.DCSVerifyCallBack;

import android.app.Activity;

public class DCSVerifyServiceHelper {

    private static DCSVerifyServiceHelper mServiceHelper = null;
    public static synchronized DCSVerifyServiceHelper getInstance() {
        if(mServiceHelper == null) {
            mServiceHelper = new DCSVerifyServiceHelper();
        }
        return mServiceHelper;
    }

    /**
     * Start DCSDepthService
     *
     * @param activity The activity to start service
     */
    public void startService(Activity activity) {
        activity.startService(new Intent(activity, DCSVerifyService.class));
//        DCSVerify.initVerify();
    }

    /**
     * Stop DCSDepthService
     * @param activity The activity to stop service
     */
    public void stopService(Activity activity) {
        activity.stopService(new Intent(activity, DCSVerifyService.class));
        DCSVerify.endVerify();
    }

    /**
     * Execute DCSDepthItem in thread pool
     *
     * @param item Depth info item
     */
    public void execute(DCSVerifyItem item) {
        DCSVerifyService.getInstance().execute(item);
    }

    public boolean setVerifyCallBack(DCSVerifyCallBack callback0) {
        if(DCSVerifyService.getInstance() != null){
            DCSVerifyService.getInstance().setVerifyCallBack(callback0);
            return true;
        } else {
            Log.e("DCSVerifyServiceHelper", "setVerifyCallBack, service not started!");
            return false;
        }
    }
}
