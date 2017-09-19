package com.dcs.verify;

public interface DCSVerifyEngineCallback {

    /*
     * When verify finished, this method would be callback
     *
     * @param tag current time in milliseconds
     * @param finalFilePath the path which save depth image data
     */
//    public void doVerifyFinish(String tag, java.lang.String finalFilePath);
    public void doVerifyFinish(String tag);
    /* do Verify error
     *
     * @param tag current time in milliseconds
     */
    public void doVerifyError(String tag);

}
