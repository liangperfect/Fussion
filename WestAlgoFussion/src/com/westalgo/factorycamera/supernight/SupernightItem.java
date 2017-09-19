package com.westalgo.factorycamera.supernight;

/**
 * Created by admin on 2017/7/6.
 */

public class SupernightItem {

    //mian item
    public byte[] mMainY; // data Y
    public byte[] mMainUV; // data UV
    public int mMainW = 0; //width
    public int mMainH = 0; //width
    public int mMainFormat = 0;
    public int mMainRotation = 0;
    public int mMain_s0 = -1; //stride[0]
    public int mMain_s1 = -1; //stride[1]

    //aux item
    public byte[] mAuxY;
    public byte[] mAuxUV;
    public int mAuxW = 0;
    public int mAuxH = 0;
    public int mAuxFormat = 0;
    public int mAuxRotation = 0;
    public int mAux_s0 = -1;//stride[0]
    public int mAux_a1 = -1;//stride[1]

    private int mRgbIso = 0;
    private int mMonoIso = 0;

    private boolean isDataOk = true;

    protected SupernightItem() {
        mMainY = null;
        mMainUV = null;
        mAuxY = null;
        mAuxUV = null;

    }


    public static SupernightItem createInstance() {

        return new SupernightItem();
    }


    public boolean empty() {

        return mMainY == null || mMainUV == null || mAuxY == null || mAuxUV == null || mMainW == 0 || mAuxW == 0;
    }

    public byte[] getMainY() {
        return mMainY;
    }

    public byte[] getMainUV() {
        return mMainUV;
    }

    public int getMainW() {
        return mMainW;
    }

    public int getMainH() {
        return mMainH;
    }

    public int getMainFormat() {
        return mMainFormat;
    }

    public int getMainRotation() {
        return mMainRotation;
    }

    public int getMain_s0() {
        return mMain_s0;
    }

    public int getMain_s1() {
        return mMain_s1;
    }

    public byte[] getAuxY() {
        return mAuxY;
    }

    public byte[] getAuxUV() {
        return mAuxUV;
    }

    public int getAuxW() {
        return mAuxW;
    }

    public int getAuxH() {
        return mAuxH;
    }

    public int getAux_s0() {
        return mAux_s0;
    }

    public int getAux_a1() {
        return mAux_a1;
    }

    public void setMainY(byte[] mainY) {
        mMainY = mainY;
    }

    public void setMainUV(byte[] mainUV) {
        mMainUV = mainUV;
    }

    public void setMainW(int mainW) {
        mMainW = mainW;
    }

    public void setMainH(int mainH) {
        mMainH = mainH;
    }

    public void setMainFormat(int mainFormat) {
        mMainFormat = mainFormat;
    }

    public void setMainRotation(int mainRotation) {
        mMainRotation = mainRotation;
    }

    public void setMain_s0(int main_s0) {
        mMain_s0 = main_s0;
    }

    public void setMain_s1(int main_s1) {
        mMain_s1 = main_s1;
    }

    public void setAuxY(byte[] auxY) {
        mAuxY = auxY;
    }

    public void setAuxUV(byte[] auxUV) {
        mAuxUV = auxUV;
    }

    public void setAuxW(int auxW) {
        mAuxW = auxW;
    }

    public void setAuxH(int auxH) {
        mAuxH = auxH;
    }

    public void setAux_s0(int aux_s0) {
        mAux_s0 = aux_s0;
    }

    public void setAux_a1(int aux_a1) {
        mAux_a1 = aux_a1;
    }

    public int getAuxFormat() {
        return mAuxFormat;
    }

    public int getAuxRotation() {
        return mAuxRotation;
    }

    public void setAuxRotation(int auxRotation) {
        mAuxRotation = auxRotation;
    }

    public int getRgbIso() {
        return mRgbIso;
    }

    public void setRgbIso(int rgbIso) {
        mRgbIso = rgbIso;
    }

    public int getMonoIso() {
        return mMonoIso;
    }

    public void setMonoIso(int monoIso) {
        mMonoIso = monoIso;
    }

    public void setIsDatOK(){
        isDataOk = isDataOk?false:true;
    }

    public boolean getIsDataOK(){
        return isDataOk;
    }
}
