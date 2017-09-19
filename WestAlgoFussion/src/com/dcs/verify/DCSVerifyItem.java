package com.dcs.verify;

public abstract class DCSVerifyItem {

    protected String tag;

    //for insert file to database
    public String title;
    public long date;
    public int length;
    public int orientation = 0;

    public int mMainImgW=0;
    public int mMainImgH=0;
    public byte[] mMainImgData=null;
    public int mMainOrientation=0;
    public int correctionMode = 0;
    public int mSubPictureFormat = 256;

    public int mSubImgW=0;
    public int mSubImgH=0;
    public byte[] mSubImgData=null;
    public int mSubOrientation=0;

//    public float[] mRefocusInfo = null;
    public float[] mVerifyInfo = null;

    protected String mDepthCacheDir=null;
    boolean mIsItemValid=false;

    protected DCSVerifyItem() {
        mMainImgData = null;
        mSubImgData = null;
        tag = System.currentTimeMillis() + "";
    }

    public DCSVerifyItem(String ttag) {

        tag = ttag;
    }

    public static DCSVerifyItem createItem() {

        return new DCSVerifyItemCache();
    }

    public static DCSVerifyItem createItem(String tag) {

        return new DCSVerifyItemCache(tag);
    }

    public void setCacheDir(String dir) {
        mDepthCacheDir = dir;
    }

    public String getCacheDir() {
        return mDepthCacheDir;
    }

    public String getTag() {
        return tag;
    }

    /**
     * Check verify item whether is empty
     */
    public boolean empty()
    {
        return mMainImgW==0 || mSubImgW==0|| mMainImgData==null|| mSubImgData==null;// || mDepthSavePath==null;
    }

    /**
     * Check verify item whether is all valid
     */
    public boolean isValid()
    {
        return mMainImgW>0&&mSubImgW>0&&mMainImgData!=null&&mSubImgData!=null&&mIsItemValid;//&&mDepthSavePath!=null;
    }

    public abstract void initLoad();

}
