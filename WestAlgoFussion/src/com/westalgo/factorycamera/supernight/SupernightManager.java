package com.westalgo.factorycamera.supernight;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import src.com.westalgo.factorycamera.supernight.FileSaver;


/**
 * Created by admin on 2017/7/6.
 */

public class SupernightManager {

    private final String TAG = this.getClass().getSimpleName();

    private SupernightThread mSupernightThread;
    private int mImageFormat = ImageFormat.JPEG;

    private Handler mainHandler;

    private boolean enableInit = false;
    private boolean enableSetData = false;
    private boolean enableGenerate = false;

    private ByteBuffer mMainYBuffer;
    private ByteBuffer mMainUVBuffer;
    private ByteBuffer mAuxYBuffer;
    private ByteBuffer mAuxUVBuffer;

    private SupernightItem mItem;
    private SupernightListener mListener;

    private final int COLOR_FORMAT_YUV_NV21 = 0x16;

    public SupernightManager() {
        mainHandler = new Handler(Looper.myLooper());
        mSupernightThread = new SupernightThread();
        mSupernightThread.start();
    }

    public interface SupernightListener {
        void onProcessFinish(String filePath);
    }

    public void setSupernihtListener(SupernightListener mListener1) {

        this.mListener = mListener1;
    }

    private int align64(int length) {
        return ((length + 63) & (~63));
    }

    public void setImgInfo(int width, int height) {
        Log.e(TAG, "westalgo :setImgInfo");
        enableInit = true;
        if (mSupernightThread != null) {
            Log.e(TAG, "westalgo:setImagInfo init supernight");
            // DcsSupernight.init(4160,3120);
            mSupernightThread.notifyDirty();
        }
    }


    public void setData(SupernightItem item, int imageFormat) {
        Log.e(TAG, "westalgo :setData");
        if (item.getIsDataOK()) {
            this.mItem = item;
            this.mImageFormat = imageFormat;
            enableSetData = true;
            if (mSupernightThread != null) {
                mSupernightThread.notifyDirty();
            }
            Log.i(TAG, "setData end");
        }
    }

    public void process() {
        Log.e(TAG, "westalgo :process");
        enableGenerate = true;
        if (mSupernightThread != null) {
            mSupernightThread.notifyDirty();
        }
    }

    private void getNv21Data() {
        if (mMainYBuffer == null) {
            Log.e(TAG, "mMainYBuffer init");
            mMainYBuffer = ByteBuffer.allocate(mItem.getMainW() * mItem.getMainH());
        }
        if (mMainUVBuffer == null) {
            Log.e(TAG, "mMainUVBuffer init");
            mMainUVBuffer = ByteBuffer.allocate(mItem.getMainW() * mItem.getMainH() / 2);
        }

        if (mAuxYBuffer == null) {
            Log.e(TAG, "mAuxYBuffer init");
            mAuxYBuffer = ByteBuffer.allocate(mItem.getAuxW() * mItem.getAuxH());
        }

        if (mAuxUVBuffer == null) {
            Log.e(TAG, "mAuxUVBuffer init");
            mAuxUVBuffer = ByteBuffer.allocate(mItem.getAuxW() * mItem.getAuxH() / 2);
        }

        if (mItem == null) {
            Log.e(TAG, "westalgo:mItem is null");
        }

        int mainYHeight = mItem.getMainH();
        int mainUVHeight = mItem.getMainH() / 2;
        int mainYWidth = mItem.getMainW();
        int mainUVWidth = mItem.getMainW();

        int auxYHeight = mItem.getAuxH();
        int auxUVHeight = mItem.getAuxH() / 2;
        int auxYWidth = mItem.getAuxW();
        int auxUVWidth = mItem.getAuxW();

        Log.e(TAG, "westalgo:mainYHeight:" + mainYHeight + " mainUVHeight:" + mainUVHeight + "  mainYWidth:" +
                mainYWidth + "  mainUVWidth:" + mainUVWidth + "  auxYHeight:" + auxYHeight +
                "  auxUVHeight:" + auxUVHeight + "  auxYWidth:" + auxYWidth + "  auxUVWidth:" + auxUVWidth);

        //dont align
        for (int i = 0; i < mainYHeight; i++) {
            mMainYBuffer.put(mItem.getMainY(), i * mainYWidth, mainYWidth);
        }
        Log.e(TAG, "westalgo:mainYBuffer put data OK");

        for (int i = 0; i < mainUVHeight; i++) {
            mMainUVBuffer.put(mItem.getMainUV(), i * mainUVWidth, mainUVWidth);
        }
        Log.e(TAG, "westalgo:mainUVBuffer put data OK");

        for (int i = 0; i < auxYHeight; i++) {
            mAuxYBuffer.put(mItem.getAuxY(), i * auxYWidth, auxYWidth);
        }
        Log.e(TAG, "westalgo:mAuxYBuffer put data OK");

        for (int i = 0; i < auxUVHeight; i++) {
            mAuxUVBuffer.put(mItem.getAuxUV(), i * auxUVWidth, auxUVWidth);
        }
        Log.e(TAG, "westalgo:mAuxUVBuffer put data OK");
        Log.e(TAG, "1111 mItem.getMainY" + mItem.getMainY().length + "mItem.getMainUV().length:" +
                mItem.getMainUV().length + "mItem.getAuxY().length:" + mItem.getAuxY().length +
                "mItem.getAuxUV().length:" + mItem.getAuxUV().length);
    }

    private void setImagePair() {
        switch (mImageFormat) {
            case ImageFormat.JPEG:
                Log.i(TAG, "no have JPEG");
                break;

            case ImageFormat.NV21:
                getNv21Data();
                break;
            default:
                // nothing to do
                break;
        }

        Log.d(TAG, "setImagePair convert end");
        int ret = DcsSupernight.setParameters(mItem.getRgbIso(), mItem.getMonoIso());
        // Log.e(TAG, "DcsSupernight Parameters ret:" + ret + "  mItem.getRgbIso()" + mItem.getRgbIso() + "  mItem.getMonoIso()" + mItem.getMonoIso());
        //if (0 == DcsSupernight.setParameters(mItem.getRgbIso(), mItem.getMonoIso())) {
        //     Log.i(TAG, "setImagePair() :" + "setParameters mainIso,auxIso end");
        // } else {
        //     Log.i(TAG, "setImagePair() :" + "setParameters error");
        //    return;
        // }

        if (0 == DcsSupernight.setImagePair(mMainYBuffer.array(), mMainUVBuffer.array(), mItem.getMainW()
                , mItem.getMainH(), COLOR_FORMAT_YUV_NV21, 0, 3120, 4160, mAuxYBuffer.array(), mAuxUVBuffer.array(), mItem.getAuxW(), mItem.getAuxH(), COLOR_FORMAT_YUV_NV21, 0, 3120, 4160)) {

            Log.i(TAG, "setImagePair() :" + "setImagePair main and aux data end");
        } else {
            Log.i(TAG, "setImagePair() :" + "setImagePair error");
            return;
        }
    }

    private void generate() {
        Log.e(TAG, "westalgo:generate start mItem.getMainH():" + mItem.getMainH() + "  mItem.getMainW():" + mItem.getMainW() + "  Item.getMainH():" + mItem.getMainH() + "  mItem.getMainW():" + mItem.getMainW());
        byte[] byteArrY = new byte[mItem.getMainH() * mItem.getMainW() * 4];
        byte[] byteArrUV = new byte[mItem.getMainH() * mItem.getMainW() * 4];
        if (0 == DcsSupernight.generate(byteArrY, byteArrUV)) {
            Log.e(TAG, "westalgo:generate() :" + "fussion picture end");
        } else {
            Log.e(TAG, "westalgo:generate() :" + "fussion picture error");
            return;
        }




        try {
            File file1 = new File("/sdrcard/main.yuv");
            //建立输出字节流
            FileOutputStream fos = new FileOutputStream(file1);
            fos.write(byteArrY);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final File file = FileSaver.getOutputMediaFile();
        boolean success = FileSaver.saveFile(3120, 4160, byteArrY, file);
        Log.d(TAG, "save end");
        if (success) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onProcessFinish(file.getPath());
                }
            });
        } else {
            Log.e(TAG, "file save error");
        }


    }

    private void startProcess() {
        if (enableInit) {
            Log.e(TAG, "thread init start");
            DcsSupernight.init(4160, 3120);
            enableInit = false;
        }
        if (enableSetData) {
            Log.e(TAG, "thread setImagePair start");
            setImagePair();
            enableSetData = false;
        }
        if (enableGenerate) {
            Log.e(TAG, "thread generate start");
            generate();
            enableGenerate = false;
        }
    }

    public void release() {
        // DcsSupernight.unDcsInit();
        if (mSupernightThread != null) {
            mSupernightThread.terminate();
            try {
                mSupernightThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mSupernightThread = null;
        }
        Log.d(TAG, "release ");
    }

    private class SupernightThread extends Thread {
        private volatile boolean mActive = true;
        private volatile boolean mDirty = false;

        public SupernightThread() {
            setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            super.run();
            while (mActive) {
                synchronized (this) {
                    if (mActive && !mDirty) {
                        Log.d(TAG, " thread wait");
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                }
                mDirty = false;
                Log.d(TAG, "thread run");
                startProcess();
            }
        }

        public synchronized void notifyDirty() {
            mDirty = true;
            notifyAll();
        }

        public synchronized void terminate() {
            mActive = false;
            notifyAll();
        }
    }

}
