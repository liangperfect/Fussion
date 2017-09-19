package com.dcs.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.westalgo.factorycamera.debug.Log;

public class DCSVerifyItemCache extends DCSVerifyItem {

    private static final Log.Tag TAG = new Log.Tag("DCSVerifyItemCache");
    static final String PATH_PROP = "props.properties";
    static final String PATH_MAIN = "main.yuv";
    static final String PATH_SUB = "sub.yuv";


    public DCSVerifyItemCache() {
        super();
    }

    public DCSVerifyItemCache(String tag) {

        super(tag);
    }

    /**
     * Write data to file storage.
     *
     * @param path The path to write data
     * @param data The data to be written.
     * @param offset The start position in data from where to get bytes.
     * @param len the number of bytes from data to write to this
     *
     * @return Return true if write data success, otherwise return false
     */
    public static boolean writeData(String path, byte[] data, int offset, int len) {
        try {
            FileOutputStream s = new FileOutputStream(path);
            s.write(data, offset, len);
            s.close();
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG,"writeData exception="+ e.toString());
            return false;
        }
    }

    /**
     * Save depth item to file storage
     *
     * @param cacheBaseDir The base directory to write item
     * @param item The depth item
     *
     * @return
     */
    public static String save(String cacheBaseDir, DCSVerifyItem item) {
        String itemDir=cacheBaseDir + "/" + item.getTag();
        Log.d(TAG,"save item dir="+itemDir);
        File dir = new File(itemDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // /////////////////////////////////

        Properties properties = new Properties();

        properties.put("left_width", item.mMainImgW+"");
        properties.put("left_height", item.mMainImgH+"");
        properties.put("left_ori", item.mMainOrientation+"");

        properties.put("right_width", item.mSubImgW+"");
        properties.put("right_height", item.mSubImgH+"");
        properties.put("right_ori", item.mSubOrientation+"");

//        StringBuffer refocus = new StringBuffer();
//        if(item.mRefocusInfo != null) {
//            for(int i = 0 ; i < item.mRefocusInfo.length; i++) {
//                refocus.append(item.mRefocusInfo[i]+",");
//            }
//        }
//        properties.put("refocus_info", refocus.toString());
//        properties.put("depth_file_append_path", item.mDepthSavePath);

        boolean saveStatus=true;
        try {
            FileOutputStream s = new FileOutputStream(itemDir + "/" + PATH_PROP);
            properties.store(s, "meta-data-stereo");
            s.flush();
            s.close();



        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG,"write prop exception="+ e.toString());
            saveStatus=false;
        }

        if(!writeData(itemDir + "/" + PATH_MAIN,item.mMainImgData, 0, item.mMainImgData.length)) {
            saveStatus=false;
        }
        if(!writeData(itemDir + "/" + PATH_SUB, item.mSubImgData, 0, item.mSubImgData.length)) {
            saveStatus=false;
        }

        item.mIsItemValid=saveStatus;
        item.setCacheDir(itemDir);

        return itemDir;
    }

    /**
     * Read data from{@code fin}
     */
    public static boolean readData(FileInputStream fin, byte[] data, int offset,
                                   int len) {
        try {
            int readedCount = 0;
            while (readedCount < len) {
                readedCount += fin.read(data, offset + readedCount, len);
            }

            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    int getIntValue(Properties properties, String key) {

        String v = properties.getProperty(key);
        int value = Integer.parseInt(v);
        return value;
    }

    float[] getFloatArrayValue(Properties properties, String key) {
        String value = properties.getProperty(key);
        if(value == null)return null;
        String fValue[] = value.split(",");
        float res[] = null;
        if(fValue!= null && fValue.length == 3) {
            res = new float[fValue.length];
            for(int i = 0 ; i < fValue.length; i++) {
                res[i] = Float.parseFloat(fValue[i]);
            }
            return res;
        }
        return null;
    }

    /**
     * Load depth item and initial it
     */
    public void initLoad() {
        if(this.mDepthCacheDir==null)
        {
            this.mIsItemValid=false;
            return ;
        }

        File itemDir=new File(mDepthCacheDir);
        if(!itemDir.exists())
        {
            this.mIsItemValid=false;
            return ;
        }

        this.mIsItemValid=true;

        Properties properties = new Properties();
        try {

            File propFile=new File(mDepthCacheDir + "/" + PATH_PROP);
            if(!propFile.exists())
            {
                this.mIsItemValid=false;
                return ;
            }


            FileInputStream s = new FileInputStream(mDepthCacheDir + "/" + PATH_PROP);
            properties.load(s);

            this.mMainImgW = getIntValue(properties, "left_width");
            this.mMainImgH = getIntValue(properties, "left_height");
            this.mMainOrientation = getIntValue(properties, "left_ori");

            this.mSubImgW = getIntValue(properties, "right_width");
            this.mSubImgH = getIntValue(properties, "right_height");
            this.mSubOrientation = getIntValue(properties, "right_ori");

//            this.mRefocusInfo = getFloatArrayValue(properties, "refocus_info");
//            this.mDepthSavePath = properties.getProperty("depth_file_append_path");

            Log.d(TAG,"DCSDepthItemCache initLoad item prop path="+mDepthCacheDir + "/" + PATH_PROP);

            s.close();

        } catch (Exception e) {
            e.printStackTrace();
            this.mIsItemValid=false;
        }

        try {

            // ///////////////////////////////////////////////////////////////
            {
                File lFile=new File(mDepthCacheDir + "/" + PATH_MAIN);
                if(!lFile.exists())
                {
                    this.mIsItemValid=false;
                    return ;
                }

                FileInputStream fleftIn = new FileInputStream(mDepthCacheDir + "/"
                        + PATH_MAIN);
                int dlen = fleftIn.available();
                this.mMainImgData = new byte[dlen];
                if(!readData(fleftIn, mMainImgData, 0, dlen)) {
                    this.mIsItemValid=false;
                }
                fleftIn.close();
            }

            // ///////////////////////////////////////////////////////////////
            {
                File rFile=new File(mDepthCacheDir + "/" + PATH_SUB);
                if(!rFile.exists())
                {
                    this.mIsItemValid=false;
                    return ;
                }
                FileInputStream frightIn = new FileInputStream(mDepthCacheDir + "/"+ PATH_SUB);
                int dlen = frightIn.available();
                this.mSubImgData = new byte[dlen];
                if(!readData(frightIn, mSubImgData, 0, dlen)) {
                    this.mIsItemValid=false;
                }
                frightIn.close();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
