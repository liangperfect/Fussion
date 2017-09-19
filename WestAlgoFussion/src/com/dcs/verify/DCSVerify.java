package com.dcs.verify;

public class DCSVerify {
    public static boolean initVerify(int lWidth, int lHeight, int rWidth, int rHeight) {
        return initVerifyNative(lWidth, lHeight, rWidth, rHeight);
    }

    public static int doVerify(long lMat, int lRotation, long rMat, int rRotation, int correctionMode){
        return doVerifyNative(lMat, lRotation, rMat, rRotation, correctionMode);
    }
    public static float getErr() {
        return getErrNative();
    }
    public static float getDistance(){
        return getDistanceNative();
    }
    public static float getDeltaY(){
        return getDeltaYNative();
    }
    public static float getOriL(){
        return getOriLNative();
    }
    public static float getDstL(){
        return getDstLNative();
    }
    public static float getOriR(){
        return getOriRNative();
    }
    public static float getDstR(){
        return getDstRNative();
    }

    public static boolean endVerify() {
        return endVerifyNative();
    }
    //get version info
    public static String getVersion(){
        return getVersionNative();
    }


    //=================================Native================================//

    /**
     * init verify parameters. Camera L: 13M RGB; Camera R: 13M mono(2M RGB).
     * Picture size rate should be 4:3.
     *
     * @param lWidth Left camera image width.
     * @param lHeight Left camera image height.
     * @param rWidth Right camera image width.
     * @param rHeight Right camera image height.
     * @return true if success, otherwise false.
     */
    private static native boolean initVerifyNative(int lWidth, int lHeight, int rWidth, int rHeight);

    /**
     * Do verify. Camera L: 13M RGB; Camera R: 13M mono(2M RGB).
     * @param lMat Left camera image Mat.
     * @param lRoation Left camera image rotation.
     * @param rMat Right camera image Mat.
     * @param rRotation Right camera image rotation.
     * @return -1 fail to do verify; 0 verify result success; 1 verify result fail.
     */
    private static native int doVerifyNative(long lMat, int lRoation, long rMat, int rRotation, int correctionMode);

    /**
     * get reprojection error value.
     * @return reprojection error value.
     */
    private static native float getErrNative();

    /**
     * get distance value.
     * @return distance value.
     */
    private static native float getDistanceNative();

    /**
     * get delta Y value.
     * @return delta Y.
     */
    private static native float getDeltaYNative();

    /**
     * get original left value.
     * @return original left.
     */
    private static native float getOriLNative();

    /**
     * get distortion left value.
     * @return distortion left value.
     */
    private static native float getDstLNative();

    /**
     * get original right value.
     * @return original right value.
     */
    private static native float getOriRNative();

    /**
     * get distortion right value.
     * @return distortion right value.
     */
    private static native float getDstRNative();

    /**
     * End verify
     * @return true if success, otherwise false.
     */
    private static native boolean endVerifyNative();

    /**
     * get version info
     * @retrun version string
     */
    private static native String getVersionNative();
}
