package com.westalgo.factorycamera.ui;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.FocusOverlayManager;
import com.westalgo.factorycamera.debug.Log;

import android.content.SharedPreferences;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class FocusOverlay extends View implements FocusOverlayManager.FocusUI {
    private static final Log.Tag TAG = new Log.Tag("FocusOverlay");
    private final static int FOCUS_DURATION_MS = 500;
    private final static int FOCUS_BACK_DURATION_MS = 1000;
    private final static int FOCUS_INDICATOR_ROTATION_DEGREES = 50;
    private final static int FOCUS_BACK_INDICATOR_ROTATION_DEGREES = 0;
    private static final int FOCUS_START = 0;
    private static final int FOCUS_SUSS = 1;
    private static final int FOCUS_FAILED = 2;

    private Drawable mFocusIndicator;
    private Drawable mFocusIndicatorSuss;
    private Drawable mFocusIndicatorFail;
    private Drawable mFocusOuterRing;
    private final Rect mBounds = new Rect();
    private final ValueAnimator mFocusStartAnimation = new ValueAnimator();

    private int mPositionX;
    private int mPositionY;
    private int mAngle;
    private final int mFocusIndicatorSize;
    private boolean mShowIndicator;
    private final int mFocusOuterRingSize;
    private int mFocusMode = FOCUS_START;
    private Drawable mCrrentIndicator;
    private boolean mIsEndAnimation = true;

    public FocusOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        mFocusIndicator = getResources().getDrawable(
                              R.drawable.focus_ring_touch_inner);
        mFocusIndicatorSuss = getResources().getDrawable(
                                  R.drawable.focus_ring_touch_inner_suss);
        mFocusIndicatorFail = getResources().getDrawable(
                                  R.drawable.focus_ring_touch_inner_fail);
        mFocusIndicatorSize = getResources().getDimensionPixelSize(
                                  R.dimen.focus_inner_ring_size);
        mFocusOuterRing = getResources().getDrawable(
                              R.drawable.focus_ring_touch_outer);
        mFocusOuterRingSize = getResources().getDimensionPixelSize(
                                  R.dimen.focus_outer_ring_size);

        mCrrentIndicator = mFocusIndicator;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mShowIndicator) {
            if(mFocusMode == FOCUS_START) {
                mCrrentIndicator = mFocusIndicator;
            } else if(mFocusMode == FOCUS_SUSS) {
                mCrrentIndicator = mFocusIndicatorSuss;
            } else if(mFocusMode == FOCUS_FAILED) {
                mCrrentIndicator = mFocusIndicatorFail;
            }
            mFocusOuterRing.draw(canvas);
            canvas.save();
            canvas.rotate(mAngle, mPositionX, mPositionY);
            mCrrentIndicator.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isLandscape()) {
            Resources resources = this.getResources();
            DisplayMetrics dm = resources.getDisplayMetrics();
            int widthPixels = dm.widthPixels;
            int heightPixels = dm.heightPixels;
            int mLandWidth = widthPixels > heightPixels ? widthPixels : heightPixels;
            SharedPreferences preferences = getContext().getSharedPreferences("DualCamera", Context.MODE_PRIVATE);
            int width = preferences.getInt("landscapePreviewWidth", mLandWidth / 2);
            boolean isAspect16to9 = preferences.getBoolean("isAspect16to9", false);
            boolean isFirstOnMeasure = true;
            int height = preferences.getInt("landscapePreviewHeight", (int)((mLandWidth / 2) * (3.0f / 4.0f)));
            if (isAspect16to9 && isFirstOnMeasure) {
                height = preferences.getInt("landscapePreviewHeight", (int)((mLandWidth / 2) * (9.0f / 16.0f)));
                isFirstOnMeasure = false;
            }
            Log.d(TAG, "----->>>onMeasure: landscapePreviewWidth = " + width + " landscapePreviewHeight = " + height
                    + " isAspect16to9 = " + isAspect16to9);
            setMeasuredDimension(width,height);
        }else {
            int height = View.MeasureSpec.getSize(heightMeasureSpec);
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(width,height);
        }
    }

    @Override
    public void onFocusStarted() {
        mIsEndAnimation = false;
        mFocusMode = FOCUS_START;
        mShowIndicator = true;
        doAnimation(FOCUS_INDICATOR_ROTATION_DEGREES, FOCUS_DURATION_MS, true);
    }

    @Override
    public void onFocusSucceeded() {
        // TODO Auto-generated method stub
        if(mIsEndAnimation)return;
        mIsEndAnimation = true;
        mFocusMode = FOCUS_SUSS;
        doAnimation(FOCUS_BACK_INDICATOR_ROTATION_DEGREES, FOCUS_BACK_DURATION_MS, false);
    }

    @Override
    public void onFocusFailed() {
        // TODO Auto-generated method stub
        if(mIsEndAnimation)return;
        mIsEndAnimation = true;
        mFocusMode = FOCUS_FAILED;
        doAnimation(FOCUS_BACK_INDICATOR_ROTATION_DEGREES, FOCUS_BACK_DURATION_MS, false);
    }

    public void doAnimation(int rotation, final int durationTime, final boolean isShow) {
        mFocusStartAnimation.setIntValues(0, rotation);
        mFocusStartAnimation.setDuration(durationTime);
        mFocusStartAnimation
        .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if(animation.getCurrentPlayTime() >= durationTime) {
                    mShowIndicator = isShow;
                }
                if(mShowIndicator) {
                    mAngle = (Integer) animation.getAnimatedValue();
                    invalidate();
                }
            }
        });
        mFocusStartAnimation.start();
    }

    @Override
    public void setFocusPosition(int x, int y) {
        setFocusPosition(x, y, 0, 0);
    }

    @Override
    public void setFocusPosition(int x, int y, int afRegineSize, int aeRegineSize) {
        mPositionX = x;
        mPositionY = y;
        mBounds.set(x - mFocusIndicatorSize / 2, y - mFocusIndicatorSize / 2, x
                    + mFocusIndicatorSize / 2, y + mFocusIndicatorSize / 2);
        mFocusIndicator.setBounds(mBounds);
        mFocusIndicatorSuss.setBounds(mBounds);
        mFocusIndicatorFail.setBounds(mBounds);
        mFocusOuterRing.setBounds(x - mFocusOuterRingSize / 2, y
                                  - mFocusOuterRingSize / 2, x + mFocusOuterRingSize / 2, y
                                  + mFocusOuterRingSize / 2);

        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        invalidate();
    }

    @Override
    public void clearFocus() {
        mShowIndicator = false;
    }

    public boolean isLandscape() {
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int screenOrientation = mConfiguration.orientation ; //获取屏幕方向

        if(screenOrientation == mConfiguration.ORIENTATION_LANDSCAPE){
            //横屏
            return true;
        }else if(screenOrientation == mConfiguration.ORIENTATION_PORTRAIT){
            //竖屏
            return false;
        }
        return false;
    }
}
