package com.westalgo.factorycamera.supernight;

/**
 * Created by liang.chen on 2017/7/5.
 */
public class DcsSupernight {

    static {
        System.loadLibrary("jni_supernight");
    }

    public static native int init(int width,int height);

    public static native int setParameters(int rgb_iso, int mono_iso);

    public static native int setImagePair(byte[] mainY, byte[] mainUV, int mainW, int mainH,
                                          int mainFormat, int mainRotation, int main_s0, int main_s1,
                                          byte[] auxY, byte[] auxUV, int auxW, int auxH, int auxForamt,
                                          int auxRotation, int aux_s0, int aux_s1);


    public static native int generate(byte[] yData,byte[] uvData);

    public static native int unInit();

    public static native String getVersion();

}
