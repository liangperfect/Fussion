package com.westalgo.factorycamera.util;

import android.os.Build;

public class ApiHelper {

    public static boolean isLOrHigher() {
//        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
//                || "L".equals(Build.VERSION.CODENAME);
        return true;
    }
}
