package com.westalgo.factorycamera.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.exif.ExifInterface;

public class BitmapUtils {

    private static final Log.Tag TAG = new Log.Tag("BitmapUtils");


    public static int MicrothumbnailTargetSize = 200;

    public static Bitmap decodeThumbnail(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        int targetSize = MicrothumbnailTargetSize;

        // try to decode from JPEG EXIF
        ExifInterface exif = new ExifInterface();
        byte[] thumbData = null;
        try {
            exif.readExif(path);
            thumbData = exif.getThumbnail();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "failed to find file to read thumbnail: " + path);
        } catch (IOException e) {
            Log.w(TAG, "failed to get thumbnail from: " + path);
        }
        if (thumbData != null) {
            Bitmap bitmap = decodeIfBigEnough(thumbData, options, targetSize);
            if (bitmap != null) return bitmap;
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            FileDescriptor fd = fis.getFD();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            int w = options.outWidth;
            int h = options.outHeight;
            float scale = (float) targetSize / Math.min(w, h);
            options.inSampleSize = computeSampleSizeLarger(scale);

            // For an extremely wide image, e.g. 300x30000, we may got OOM when
            // decoding
            // it for TYPE_MICROTHUMBNAIL. So we add a max number of pixels
            // limit here.
            final int MAX_PIXEL_COUNT = 160000; // 400 x 400
            if ((w / options.inSampleSize) * (h / options.inSampleSize) > MAX_PIXEL_COUNT) {
                options.inSampleSize = computeSampleSize((float) Math.sqrt((float) MAX_PIXEL_COUNT / (w * h)));
            }

            options.inJustDecodeBounds = false;
            Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
            if (result == null) return null;

            return resizeAndCropCenter(result, targetSize, true);
        } catch (Exception ex) {
            Log.w(TAG, "" + ex);
            return null;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Decodes the bitmap from the given byte array if the image size is larger than the given
     * requirement.
     *
     * Note: The returned image may be resized down. However, both width and height must be
     * larger than the <code>targetSize</code>.
     */
    public static Bitmap decodeIfBigEnough(byte[] data,
                                           Options options, int targetSize) {
        if (options == null) options = new Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (options.outWidth < targetSize || options.outHeight < targetSize) {
            return null;
        }
        options.inSampleSize = computeSampleSizeLarger(
                                   options.outWidth, options.outHeight, targetSize);
        options.inJustDecodeBounds = false;

        return ensureGLCompatibleBitmap(
                   BitmapFactory.decodeByteArray(data, 0, data.length, options));
    }

    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) return bitmap;
        Bitmap newBitmap = bitmap.copy(Config.ARGB_8888, false);
        bitmap.recycle();
        return newBitmap;
    }


    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static Bitmap resizeBitmapByScale(
        Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == size && h == size) return bitmap;

        // scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped.
        float scale = (float) size / Math.min(w,  h);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    // Find the max x that 1 / x <= scale.
    public static int computeSampleSize(float scale) {
        int initialSize = Math.max(1, (int) Math.ceil(1 / scale));
        return initialSize <= 8 ? nextPowerOf2(initialSize)
               : (initialSize + 7) / 8 * 8;
    }

    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1f / scale);
        if (initialSize <= 1)
            return 1;

        return initialSize <= 8 ? prevPowerOf2(initialSize)
               : initialSize / 8 * 8;
    }

    public static int computeSampleSizeLarger(int w, int h,
            int minSideLength) {
        int initialSize = Math.max(w / minSideLength, h / minSideLength);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
               ? prevPowerOf2(initialSize)
               : initialSize / 8 * 8;
    }

    public static int prevPowerOf2(int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30))
            throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    public static Bitmap getThumbFromDatabase(com.westalgo.factorycamera.CameraActivity activity) {
        String selection = MediaStore.Images.Media.DATA + " like ?";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                .getPath() + "/Camera/";
        String[] selectionArgs = {path + "%"};
        Uri originalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = activity.getContentResolver();
        Cursor cursor = cr.query(originalUri, null, selection, selectionArgs,
                MediaStore.Images.Media.DATE_TAKEN + " desc");
        Bitmap bitmap = null;
        if (cursor != null && cursor.moveToFirst()) {
            long thumbNailsId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images
                    .ImageColumns._ID));
            String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images
                    .ImageColumns.DATA));
            //generate uri
            Uri uri = Uri.parse("content://media/external/images/media/");
            uri = ContentUris.withAppendedId(uri, thumbNailsId);
            activity.updateCurrentImgUri(uri);
            bitmap = MediaStore.Images.Thumbnails.getThumbnail(cr,
                    thumbNailsId, MediaStore.Images.Thumbnails.MICRO_KIND, null);
        }
        cursor.close();
        return bitmap;
    }
}
