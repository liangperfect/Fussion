package com.dcs.verify;

import java.io.File;
import java.util.LinkedList;
import com.westalgo.factorycamera.debug.Log;

public class DCSVerifyQueue {
    private static final Log.Tag TAG = new Log.Tag("DCSVerifyQueue");
    private Object obj = new Object();

    LinkedList<DCSVerifyItem> mItemList = new LinkedList<DCSVerifyItem>();

    String cacheDir = null;

    public DCSVerifyQueue(String cacheDirPath) {
        cacheDir = cacheDirPath + "/pre_depth";
        File file = new File(cacheDir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * Iterate file directory to save depth item to list
     */
    void initCacheLoad() {
        File dir = new File(cacheDir);
        Log.d(TAG,"initCacheLoad cache path="+cacheDir);
        if (dir.exists()) {
            File[] dirs = dir.listFiles();
            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    if (dirs[i].isDirectory()) {
                        DCSVerifyItem item = DCSVerifyItem.createItem(dirs[i]
                                            .getName());
                        item.setCacheDir(dirs[i].getAbsolutePath());
                        synchronized(obj) {
                            mItemList.addLast(item);
                        }
                        Log.d(TAG,"initCacheLoad cache item "+dirs[i].getAbsolutePath()+" item name="+dirs[i].getName());
                    }
                }
            }
        }

    }

    void cacheDepthItem(DCSVerifyItem item) {
        DCSVerifyItemCache.save(cacheDir, item);
    }

    /**
     * Enqueue depth item to file storage
     *
     * @param item The depth item
     */
    void enqueue(final DCSVerifyItem item) {
        DCSThreadManager.instance().add(new Runnable() {
            @Override
            public void run() {
                cacheDepthItem(item);
                synchronized(obj) {
                    mItemList.addLast(item);
                }
            }

        });
    }

    /**
     * Load depth item from file storage
     *
     * @param item The depth item which will be filled
     */
    void loadDepthItemData(DCSVerifyItem item) {
        item.initLoad();
    }

    /**
     * Remove depth item from disk(file storage)
     *
     * @param item The depth item
     */
    void removeDiskDepthItem(DCSVerifyItem item) {

        if(item.getCacheDir()==null)return ;
        File dir = new File(item.getCacheDir());
        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].exists()) {
                        files[i].delete();
                    }
                }
            }
            dir.delete();
        }
    }

    /**
     * Dequeue a last depth item
     *
     * @return Return the last depth item
     */
    DCSVerifyItem dequeue() {
        DCSVerifyItem q = null;
        while(q == null) {
            if (!mItemList.isEmpty()) {
                synchronized(obj) {
                    q = (DCSVerifyItem) mItemList.removeLast();
                }
            } else {
                Log.d(TAG,"dequeue  isEmpty");
                break;
            }
        }
        return q;
    }

    /**
     * Returns if this list contains no elements
     *
     * @return Return true if this list has no elements, otherwise false
     */
    boolean hasMore() {
        return !mItemList.isEmpty();
    }

}
