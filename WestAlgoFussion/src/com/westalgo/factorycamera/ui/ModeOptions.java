/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.westalgo.factorycamera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.module.DualBaseCameraModule;
import com.westalgo.factorycamera.settings.SettingUtil;
import com.westalgo.factorycamera.ui.MultiToggleImageButton;
import com.westalgo.factorycamera.ui.RadioOptions;
import com.westalgo.factorycamera.ui.TopRightWeightedLayout;
import com.westalgo.factorycamera.util.Gusterpolator;

import java.util.ArrayList;

public class ModeOptions extends FrameLayout {
    private static final String TAG = "ModeOptions";
    private int mBackgroundColor;
    private final Paint mPaint = new Paint();
    private boolean mIsHiddenOrHiding;
    private RectF mAnimateFrom = new RectF();
    private View mViewToShowHide;
    private TopRightWeightedLayout mModeOptionsButtons;
    private RadioOptions mModeOptionsExposure;
    private HorizontalListView mModeOptionsISO;

    private AnimatorSet mVisibleAnimator;
    private AnimatorSet mHiddenAnimator;
    private boolean mDrawCircle;
    private boolean mFill;
    private static final int RADIUS_ANIMATION_TIME = 250;
    private static final int SHOW_ALPHA_ANIMATION_TIME = 350;
    private static final int HIDE_ALPHA_ANIMATION_TIME = 200;
    private static final int PADDING_ANIMATION_TIME = 350;

    private ViewGroup mMainBar;
    private ViewGroup mActiveBar;
    public static final int BAR_INVALID = -1;
    public static final int BAR_STANDARD = 0;
    public static final int BAR_PANO = 1;

    private boolean mIsPortrait;
    private float mRadius = 0f;

    Context mContext;
    ISOSettingAdapter mIsoSettingAdapter;
    OnISOOptionClickListener mISOOptionClickListener;
    String lastISOSetting;

    public interface OnISOOptionClickListener{
        public void onISOOptionClicked(String value);
    }
    public void setISOClickListener(OnISOOptionClickListener mClickListener){
        mISOOptionClickListener = mClickListener;
    }

    public ModeOptions(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mContext == null) {
            mContext = context;
        }
    }

    public void setViewToShowHide(View v) {
        mViewToShowHide = v;
    }

    @Override
    public void onFinishInflate() {
        mIsHiddenOrHiding = true;
        mBackgroundColor = getResources().getColor(R.color.mode_options_background);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mBackgroundColor);
        mModeOptionsButtons = (TopRightWeightedLayout) findViewById(R.id.mode_options_buttons);
        mModeOptionsExposure = (RadioOptions) findViewById(R.id.mode_options_exposure);
        mModeOptionsISO = (HorizontalListView) findViewById(R.id.iso_list);
        mMainBar = mActiveBar = mModeOptionsButtons;

        ImageButton exposureButton = (ImageButton) findViewById(R.id.exposure_button);
        exposureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveBar = mModeOptionsExposure;
                mMainBar.setVisibility(View.INVISIBLE);
                mActiveBar.setVisibility(View.VISIBLE);
            }
        });
        ImageButton isoButton = (ImageButton) findViewById(R.id.iso_button);
        isoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActiveBar = mModeOptionsISO;
                //update iso setting when iso button clicked.
                if (SettingUtil.SUPPORTED_ISO != null && SettingUtil.SUPPORTED_ISO != "") {
                    if (SettingUtil.forceUpdateISO || ISOSettingAdapter.ISO_SUPPORTED_STRING == null
                            || !ISOSettingAdapter.ISO_SUPPORTED_STRING.equalsIgnoreCase(SettingUtil.SUPPORTED_ISO)) {
                        Log.d(TAG, "isoButton clicked, update iso setting items.");
                        ISOSettingAdapter.ISO_SUPPORTED_STRING = SettingUtil.SUPPORTED_ISO;
                        mIsoSettingAdapter.updateISOItems();
                        SettingUtil.forceUpdateISO = false;
                        if (lastISOSetting != null) {
                            int index = mIsoSettingAdapter.getISOValueIndex(lastISOSetting);
                            Log.d(TAG, "isoButton clicked, update selected value index: "+index);
                            if (index >= 0) {
                                mModeOptionsISO.setSelection(index);
                                onListItemSelected(mIsoSettingAdapter);
                            }
                        }
                    }
                }
                mMainBar.setVisibility(View.INVISIBLE);
                mActiveBar.setVisibility(View.VISIBLE);
            }
        });
        mIsoSettingAdapter = new ISOSettingAdapter(mContext);
        mIsoSettingAdapter.createFilters();
        mModeOptionsISO.setAdapter(mIsoSettingAdapter);
        mIsoSettingAdapter.setListParent(mModeOptionsISO);
        onListItemSelected(mIsoSettingAdapter);
        mModeOptionsISO.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int preSelected = parent.getSelectedItemPosition();
                if (preSelected == position || mIsoSettingAdapter.getItem(position) == null) {
                    return;
                }
                Log.d(TAG, "-->onItemClick, iso pos = " + position);
                mModeOptionsISO.setSelection(position);
                mISOOptionClickListener.onISOOptionClicked(mIsoSettingAdapter.getSelectedTagValue());
                onListItemSelected(mIsoSettingAdapter);
            }
        });
        mModeOptionsISO.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> list, View v, int position, long id) {
                if (mIsoSettingAdapter.getItem(position) == null) {
                    return;
                }
                Log.d(TAG, "-->onItemSelected, iso pos = " + position);
                mModeOptionsISO.setSelection(position);
                mISOOptionClickListener.onISOOptionClicked(mIsoSettingAdapter.getSelectedTagValue());
                onListItemSelected(mIsoSettingAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> list) {
                Log.w(TAG, "onNothingSelected ");
                if (list.getAdapter().getCount() > 0) {
                    list.setSelection(0);
                    Log.d(TAG, "onNothingSelected, set selection to 0.");
                }
            }
        });
    }

    public boolean isISOOptionVisiable(){
        return (mActiveBar == mModeOptionsISO && mModeOptionsISO.getVisibility() == View.VISIBLE);
    }

    public void initISOSettings(String isoValue) {
        if (mISOOptionClickListener == null || mModeOptionsISO == null || mIsoSettingAdapter == null) {
            Log.w(TAG, "initISOSettings failed, not init.");
            return;
        }
        lastISOSetting = isoValue;
        int index = mIsoSettingAdapter.getISOValueIndex(isoValue);
        Log.d(TAG, "initISOSetting, value index: "+index);
        if (index >= 0) {
            mModeOptionsISO.setSelection(index);
            onListItemSelected(mIsoSettingAdapter);
        }
    }

    private void onListItemSelected(ISOSettingAdapter filterAdapter) {
        if (filterAdapter == null)
            return;
        filterAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isISOOptionVisiable()) {
            return mModeOptionsISO.dispatchTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public void setMainBar(int b) {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setVisibility(View.INVISIBLE);
        }
        switch (b) {
        case BAR_STANDARD:
            mMainBar = mActiveBar = mModeOptionsButtons;
            break;
        }
        mMainBar.setVisibility(View.VISIBLE);
    }

    public int getMainBar() {
        if (mMainBar == mModeOptionsButtons) {
            return BAR_STANDARD;
        }

        return BAR_INVALID;
    }

    @Override
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE && !mIsHiddenOrHiding) {
            // Collapse mode options when window is not visible.
            setVisibility(INVISIBLE);
            if (mMainBar != null) {
                mMainBar.setVisibility(VISIBLE);
            }
            if (mActiveBar != null && mActiveBar != mMainBar) {
                mActiveBar.setVisibility(INVISIBLE);
            }
            if (mViewToShowHide != null) {
                mViewToShowHide.setVisibility(VISIBLE);
            }
            mIsHiddenOrHiding = true;
        }
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            mIsPortrait = (getResources().getConfiguration().orientation ==
                           Configuration.ORIENTATION_PORTRAIT);

            int buttonSize = getResources()
                             .getDimensionPixelSize(R.dimen.option_button_circle_size);
            int buttonPadding = getResources()
                                .getDimensionPixelSize(R.dimen.mode_options_toggle_padding);

            float rLeft, rRight, rTop, rBottom;
            if (mIsPortrait) {
                rLeft = getWidth() - buttonPadding - buttonSize;
                rTop = (getHeight() - buttonSize) / 2.0f;
            } else {
                rLeft = buttonPadding;
                rTop = buttonPadding;
            }
            rRight = rLeft + buttonSize;
            rBottom = rTop + buttonSize;
            mAnimateFrom.set(rLeft, rTop, rRight, rBottom);

            setupAnimators();
            setupToggleButtonParams();
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mDrawCircle) {
            canvas.drawCircle(mAnimateFrom.centerX(), mAnimateFrom.centerY(), mRadius, mPaint);
        } else if (mFill) {
            canvas.drawPaint(mPaint);
        }
        super.onDraw(canvas);
    }

    private void setupToggleButtonParams() {
        int size = (mIsPortrait ? getHeight() : getWidth());

        for (int i = 0; i < mModeOptionsButtons.getChildCount(); i++) {
            View button = mModeOptionsButtons.getChildAt(i);
            if (button instanceof MultiToggleImageButton) {
                MultiToggleImageButton toggleButton = (MultiToggleImageButton) button;
                toggleButton.setParentSize(size);
                toggleButton.setAnimDirection(mIsPortrait ?
                                              MultiToggleImageButton.ANIM_DIRECTION_VERTICAL :
                                              MultiToggleImageButton.ANIM_DIRECTION_HORIZONTAL);
            }
        }
    }

    private void setupAnimators() {
        if (mVisibleAnimator != null) {
            mVisibleAnimator.end();
        }
        if (mHiddenAnimator != null) {
            mHiddenAnimator.end();
        }

        final float fullSize = (mIsPortrait ? (float) getWidth() : (float) getHeight());

        // show
        {
            final ValueAnimator radiusAnimator =
                ValueAnimator.ofFloat(mAnimateFrom.width()/2.0f,
                                      fullSize-mAnimateFrom.width()/2.0f);
            radiusAnimator.setDuration(RADIUS_ANIMATION_TIME);
            radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mRadius = (Float) animation.getAnimatedValue();
                    mDrawCircle = true;
                    mFill = false;
                }
            });
            radiusAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDrawCircle = false;
                    mFill = true;
                }
            });

            final ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            alphaAnimator.setDuration(SHOW_ALPHA_ANIMATION_TIME);
            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mActiveBar.setAlpha((Float) animation.getAnimatedValue());
                }
            });
            alphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mActiveBar.setAlpha(1.0f);
                }
            });

            final int deltaX = getResources()
                               .getDimensionPixelSize(R.dimen.mode_options_buttons_anim_delta_x);
            int childCount = mActiveBar.getChildCount();
            ArrayList<Animator> paddingAnimators = new ArrayList<Animator>();
            for (int i = 0; i < childCount; i++) {
                final View button;
                if (mIsPortrait) {
                    button = mActiveBar.getChildAt(i);
                } else {
                    button = mActiveBar.getChildAt(childCount-1-i);
                }

                final ValueAnimator paddingAnimator =
                    ValueAnimator.ofFloat(deltaX*(childCount-i), 0.0f);
                paddingAnimator.setDuration(PADDING_ANIMATION_TIME);
                paddingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (mIsPortrait) {
                            button.setTranslationX((Float) animation.getAnimatedValue());
                        } else {
                            button.setTranslationY(-((Float) animation.getAnimatedValue()));
                        }
                        invalidate();
                    }
                });

                paddingAnimators.add(paddingAnimator);
            }

            AnimatorSet paddingAnimatorSet = new AnimatorSet();
            paddingAnimatorSet.playTogether(paddingAnimators);

            mVisibleAnimator = new AnimatorSet();
            mVisibleAnimator.setInterpolator(Gusterpolator.INSTANCE);
            mVisibleAnimator.playTogether(radiusAnimator, alphaAnimator, paddingAnimatorSet);
        }

        // hide
        {
            final ValueAnimator radiusAnimator =
                ValueAnimator.ofFloat(fullSize-mAnimateFrom.width()/2.0f,
                                      mAnimateFrom.width()/2.0f);
            radiusAnimator.setDuration(RADIUS_ANIMATION_TIME);
            radiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mRadius = (Float) animation.getAnimatedValue();
                    mDrawCircle = true;
                    mFill = false;
                    invalidate();
                }
            });
            radiusAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mViewToShowHide != null) {
                        mViewToShowHide.setVisibility(View.VISIBLE);
                        mDrawCircle = false;
                        mFill = false;
                        invalidate();
                    }
                }
            });

            final ValueAnimator alphaAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
            alphaAnimator.setDuration(HIDE_ALPHA_ANIMATION_TIME);
            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mActiveBar.setAlpha((Float) animation.getAnimatedValue());
                    invalidate();
                }
            });
            alphaAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.INVISIBLE);
                    if (mActiveBar != mMainBar) {
                        mActiveBar.setAlpha(1.0f);
                        mActiveBar.setVisibility(View.INVISIBLE);
                    }
                    mMainBar.setAlpha(1.0f);
                    mMainBar.setVisibility(View.VISIBLE);
                    mActiveBar = mMainBar;
                    invalidate();
                }
            });

            mHiddenAnimator = new AnimatorSet();
            mHiddenAnimator.setInterpolator(Gusterpolator.INSTANCE);
            mHiddenAnimator.playTogether(radiusAnimator, alphaAnimator);
        }
    }

    public void animateVisible() {
        if (mIsHiddenOrHiding) {
            if (mViewToShowHide != null) {
                mViewToShowHide.setVisibility(View.INVISIBLE);
            }
            mHiddenAnimator.cancel();
            mVisibleAnimator.end();
            setVisibility(View.VISIBLE);
            mVisibleAnimator.start();
        }
        mIsHiddenOrHiding = false;
    }

    public void animateHidden() {
        if (!mIsHiddenOrHiding) {
            mVisibleAnimator.cancel();
            mHiddenAnimator.end();
            mHiddenAnimator.start();
        }
        mIsHiddenOrHiding = true;
    }

}