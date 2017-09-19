/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.westalgo.factorycamera;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore.Video;
import android.util.Log;

import com.westalgo.factorycamera.exif.ExifInterface;
import com.westalgo.factorycamera.exif.ExifTag;
import com.westalgo.factorycamera.util.CameraUtil;

/*
 * Service for saving images in the background thread.
 */
public class MediaSaveService extends Service {
    public static final String VIDEO_BASE_URI = "content://media/external/video/media";

    // The memory limit for unsaved image is 50MB.
    private static final int SAVE_TASK_MEMORY_LIMIT = 50 * 1024 * 1024;
    private static final String TAG = "CAM_" + MediaSaveService.class.getSimpleName();
    private boolean isSubSaved = false;
    private boolean isMainSaved = false;
    private String zipFileNameStr = null;
    private int BUFF_SIZE = 1024 * 1024 * 10;
    ZipOutputStream zipOutputStream;
    File zipFile = null;

    private static final String YML_CALI_PATH = "/sdcard/CaliParams.yml";
    private static final String OPT_CALI_PATH = "/data/dualcam_cali.bin";
    private static final String MANAUL_CALI_PATH = "/sdcard/dualcam_cali.bin";

    private final IBinder mBinder = new LocalBinder();
    private Listener mListener;
    // Memory used by the total queued save request, in bytes.
    private long mMemoryUse;

    public interface Listener {
        public void onQueueStatus(boolean full);
    }

    public interface OnMediaSavedListener {
        public void onMediaSaved(Uri uri);
    }

    class LocalBinder extends Binder {
        public MediaSaveService getService() {
            return MediaSaveService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public void onCreate() {
        CameraUtil.checkCameraFolder();
        CameraUtil.checkDebugFolder();
        mMemoryUse = 0;
    }

    public boolean isQueueFull() {
        return (mMemoryUse >= SAVE_TASK_MEMORY_LIMIT);
    }

    public void addImage(final byte[] data, String title, long date, Location loc,
                         int width, int height, int orientation, ExifInterface exif,
                         OnMediaSavedListener l, ContentResolver resolver, String pictureFormat) {
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        ImageSaveTask t = new ImageSaveTask(data, title, date,
                                            (loc == null) ? null : new Location(loc),
                                            width, height, orientation, exif, resolver, l, pictureFormat);

        mMemoryUse += data.length;
        if (isQueueFull()) {
            onQueueFull();
        }
        t.execute();
    }

    public void addImage(final byte[] data, String title, long date, Location loc,
                         int orientation, ExifInterface exif,
                         OnMediaSavedListener l, ContentResolver resolver) {
        // When dimensions are unknown, pass 0 as width and height,
        // and decode image for width and height later in a background thread
        addImage(data, title, date, loc, 0, 0, orientation, exif, l, resolver,
                 "jpeg");
    }
    public void addImage(final byte[] data, String title, Location loc,
                         int width, int height, int orientation, ExifInterface exif,
                         OnMediaSavedListener l, ContentResolver resolver) {
        addImage(data, title, System.currentTimeMillis(), loc, width, height,
                 orientation, exif, l, resolver,"jpeg");
    }

    public void addVideo(String path, long duration, ContentValues values,
                         OnMediaSavedListener l, ContentResolver resolver) {
        // We don't set a queue limit for video saving because the file
        // is already in the storage. Only updating the database.
        new VideoSaveTask(path, duration, values, l, resolver).execute();
    }

    public void setListener(Listener l) {
        mListener = l;
        if (l == null) return;
        l.onQueueStatus(isQueueFull());
    }

    private void onQueueFull() {
        if (mListener != null) mListener.onQueueStatus(true);
    }

    private void onQueueAvailable() {
        if (mListener != null) mListener.onQueueStatus(false);
    }

    private class ImageSaveTask extends AsyncTask <Void, Void, Uri> {
        private byte[] data;
        private String title;
        private long date;
        private Location loc;
        private int width, height;
        private int orientation;
        private ExifInterface exif;
        private ContentResolver resolver;
        private OnMediaSavedListener listener;
        private String pictureFormat;

        public ImageSaveTask(byte[] data, String title, long date, Location loc,
                             int width, int height, int orientation, ExifInterface exif,
                             ContentResolver resolver, OnMediaSavedListener listener, String pictureFormat) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            this.pictureFormat = pictureFormat;
        }

        @Override
        protected void onPreExecute() {
            // do nothing.
        }

        @Override
        protected Uri doInBackground(Void... v) {
            if (width == 0 || height == 0) {
                // Decode bounds
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                width = options.outWidth;
                height = options.outHeight;
            }

            Uri uri = Storage.addImage(resolver, title, date, loc, orientation, exif, data, width, height, pictureFormat);
            if (uri != null && CameraUtil.DEBUG_MODE) {
                if (pictureFormat.contains("yuv")) {
                    //save sub
                    isSubSaved = true;
                    generateZipOutputStream();
                    if (zipOutputStream != null) {
                        try {
                            File sub = new File(Storage.generateFilepath(title, null));
                            zipFile(sub, zipOutputStream, "");
                            if (isMainSaved) {
                                File cali = getCaliFile();
                                if (cali != null) {
                                    zipFile(cali, zipOutputStream, "");
                                }
                                zipOutputStream.close();
                                zipOutputStream = null;
                                zipFileNameStr = null;
                                isMainSaved = false;
                                isSubSaved = false;
                            }
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return uri;
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return uri;
                        }
                    }
                } else if (pictureFormat.contains("jpeg")) {
                    isMainSaved = true;
                    generateZipOutputStream();
                    if (zipOutputStream != null) {
                        File main = new File(Storage.generateFilepath(title, null));
                        try {
                            zipFile(main, zipOutputStream, "");
                            if (isSubSaved) {
                                File cali = getCaliFile();
                                if (cali != null) {
                                    zipFile(cali, zipOutputStream, "");
                                }
                                zipOutputStream.close();
                                zipOutputStream = null;
                                zipFileNameStr = null;
                                isMainSaved = false;
                                isSubSaved = false;
                            }
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return uri;
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return uri;
                        }
                    }
                }
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) listener.onMediaSaved(uri);
            boolean previouslyFull = isQueueFull();
            mMemoryUse -= data.length;
            if (isQueueFull() != previouslyFull) onQueueAvailable();
            this.data = null;
        }
    }


    private class VideoSaveTask extends AsyncTask <Void, Void, Uri> {
        private String path;
        private long duration;
        private ContentValues values;
        private OnMediaSavedListener listener;
        private ContentResolver resolver;

        public VideoSaveTask(String path, long duration, ContentValues values,
                             OnMediaSavedListener l, ContentResolver r) {
            this.path = path;
            this.duration = duration;
            this.values = new ContentValues(values);
            this.listener = l;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            values.put(Video.Media.SIZE, new File(path).length());
            values.put(Video.Media.DURATION, duration);
            Uri uri = null;
            try {
                Uri videoTable = Uri.parse(VIDEO_BASE_URI);
                uri = resolver.insert(videoTable, values);

                // Rename the video file to the final name. This avoids other
                // apps reading incomplete data.  We need to do it after we are
                // certain that the previous insert to MediaProvider is completed.
                String finalName = values.getAsString(
                                       Video.Media.DATA);
                if (new File(path).renameTo(new File(finalName))) {
                    path = finalName;
                }

                resolver.update(uri, values, null, null);
            } catch (Exception e) {
                // We failed to insert into the database. This can happen if
                // the SD card is unmounted.
                Log.e(TAG, "failed to add video to media store", e);
                uri = null;
            } finally {
                Log.v(TAG, "Current video URI: " + uri);
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) listener.onMediaSaved(uri);
        }
    }
    
    private void generateZipOutputStream(){
        if (zipFileNameStr == null) {
            zipFileNameStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(System.currentTimeMillis()));
        }
        File dir = new File("sdcard/camera/capture");
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        if (zipFile == null) {
            zipFile = new File("sdcard/camera/capture/" + zipFileNameStr + ".zip");
        }
        if (zipFile != null && zipOutputStream == null) {
            try {
                zipOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFF_SIZE));
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                zipFileNameStr = null;
                zipFile = null;
                zipOutputStream = null;
            }
        }
    }

    //save zip file
    private void zipFile(File resFile, ZipOutputStream zipout, String rootpath)
            throws FileNotFoundException, IOException {
          int BUFF_SIZE = 1024 * 1024 * 10;
          rootpath = rootpath + (rootpath.trim().length() == 0 ? "" : File.separator)
              + resFile.getName();
          rootpath = new String(rootpath.getBytes("8859_1"), "GB2312");
          if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles();
            for (File file : fileList) {
              zipFile(file, zipout, rootpath);
            }
          } else {
            byte buffer[] = new byte[BUFF_SIZE];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(resFile),
                BUFF_SIZE);
            zipout.putNextEntry(new ZipEntry(rootpath));
            int realLength;
            while ((realLength = in.read(buffer)) != -1) {
              zipout.write(buffer, 0, realLength);
            }
            in.close();
            zipout.flush();
            zipout.closeEntry();
          }
    }

    //get calibration file
    private File getCaliFile(){
        File caliYML = new File(MANAUL_CALI_PATH);
        if (caliYML.exists()) {
            return caliYML;
        } else {
            File caliOPT = new File(OPT_CALI_PATH);
            if (caliOPT.exists()) {
                return caliOPT;
            } else {
                return null;
            }
        }
    }
}
