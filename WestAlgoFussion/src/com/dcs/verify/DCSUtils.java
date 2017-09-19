package com.dcs.verify;

import org.opencv.core.*;
import org.opencv.android.Utils;

import android.graphics.Bitmap;
import com.westalgo.factorycamera.debug.Log;

public class DCSUtils {

    private static final Log.Tag TAG = new Log.Tag("DCSUtils");
    // the aperture value
    private static float[] mAperture = { 16.0f, 11.0f, 8.0f, 5.6f, 4.0f, 2.8f,
                                         2.0f, 1.4f, 1.0f
                                       };

    /**
     * @param src The source mat
     *
     * @return Return bitmap
     */
    public static Bitmap Mat2Bitmap(Mat src) {
        Bitmap bmp = Bitmap.createBitmap(src.cols(), src.rows(),
                                         Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bmp, true);
        return bmp;
    }

    /**
     * @param bitmap The source bitmap
     *
     * @return Return bitmap in mat type
     */
    public static Mat bitmap2Mat(Bitmap bitmap) {

        Mat dst = new Mat();
        Utils.bitmapToMat(bitmap, dst);
        return dst;
    }

    /**
     * Convert the blur range(0~100) to aperture value (16.0, 11.0, 8.0, 5.6,
     * 4.0, 2.8, 2.0, 1.4, 1.0)
     *
     * @param range The range of 0-100
     *
     * @return Return aperture value
     */
    public static float blurRangeToAperture(int range) {
        if (range > 100 || range < 0) {
            Log.e(TAG, "the blur range value error! Return 16.0 f-number");
            return mAperture[0];
        }
        float tmpValue = (float) range / 100 * mAperture.length;
        int apertureId = (int) ((float) range / 100 * mAperture.length);
        if (apertureId >= mAperture.length - 1) {
            apertureId = mAperture.length - 1;
        }
        float aperture = mAperture[apertureId];
        Log.i(TAG, "Aperture length: " + mAperture.length + ", range: " + range
              + ", tmpValue: " + tmpValue + ", convert value: " + apertureId
              + ", apeture: " + aperture);
        return aperture;
    }

}
