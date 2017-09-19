package com.westalgo.factorycamera.app;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Camera.CameraInfo;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageButton;

import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.ButtonManager;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.manager.CameraHolder;
import com.westalgo.factorycamera.module.DualTextureViewHelper;
import com.westalgo.factorycamera.module.ModuleController;
import com.westalgo.factorycamera.module.ModulesInfo;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.ui.BottomBar;
import com.westalgo.factorycamera.ui.BottomBarModeOptionsWrapper;
import com.westalgo.factorycamera.ui.MainActivityLayout;
import com.westalgo.factorycamera.ui.ModeListView;
import com.westalgo.factorycamera.ui.ModeOptionsOverlay;
import com.westalgo.factorycamera.ui.ModeTransitionView;
import com.westalgo.factorycamera.ui.PreviewOverlay;
import com.westalgo.factorycamera.ui.PreviewStatusListener;
import com.westalgo.factorycamera.ui.ShutterButton;
import com.westalgo.factorycamera.ui.MainActivityLayout.NonDecorWindowSizeChangedListener;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.wiget.IndicatorIconController;

public class CameraAppUI implements ModeListView.ModeSwitchListener, View.OnClickListener,
    NonDecorWindowSizeChangedListener {

    private static final Log.Tag TAG = new Log.Tag("CameraAppUI");
    // Mode cover states:
    private final static int COVER_HIDDEN = 0;
    private final static int COVER_SHOWN = 1;
    private int mModeCoverState = COVER_HIDDEN;

    private final static int SWIPE_TIME_OUT_MS = 500;
    // Swipe states:
    private final static int IDLE = 0;
    private final static int SWIPE_UP = 1;
    private final static int SWIPE_DOWN = 2;
    private final static int SWIPE_RIGHT = 4;
    private int mSwipeState = IDLE;

    private MainActivityLayout mAppRootView;
    private AppController mAppController;
    private ModeListView mModeListView;
    private final ModeTransitionView mModeTransitionView;
    private PreviewOverlay mPreviewOverlay;
    private PreviewStatusListener mPreviewStatusListener;
    private GestureDetector mGestureDetector;

    private Runnable mHideCoverRunnable;

    private BottomBarModeOptionsWrapper mBottomBarWrapper;
    private ShutterButton mShutterButton;
    private BottomBar mBottomBar;
    private ImageButton switchButton;

    private int mSlop;
    private boolean mSwipeEnabled = true;
    private Rect mPreviewRect = new Rect(0, 0, 0, 0);
    private float mAspectRatio = DualTextureViewHelper.MATCH_SCREEN;
    private Rect mBottomBarRect = new Rect(0, 0, 0, 0);
    private int mBottomBarMinHeight;
    private int mBottomBarMaxHeight;
    private int mBottomBarOptionHeigth;

    private int mWindowWidth;
    private int mWindowHeight;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private boolean mShouldBottomBarTopOnPreview = false;
    private IndicatorIconController mIndicatorIconController;

    public CameraAppUI(AppController controller, View appRootView) {
        mSlop = ViewConfiguration.get(controller.getAndroidContext())
                .getScaledTouchSlop();
        mAppRootView = (MainActivityLayout) appRootView;
        mAppController = controller;
        mModeListView = (ModeListView) appRootView
                        .findViewById(R.id.mode_list_layout);
        if (mModeListView != null) {
            mModeListView.setModeSwitchListener(this);
            mModeListView.setCameraAppUI(this);
        }
        mModeTransitionView = (ModeTransitionView) mAppRootView
                              .findViewById(R.id.mode_transition_view);
        mGestureDetector = new GestureDetector(controller.getAndroidContext(),
                                               new MyGestureListener());
        mAppRootView.setNonDecorWindowSizeChangedListener(this);

        mBottomBarOptionHeigth = mAppRootView.getResources()
                                 .getDimensionPixelSize(R.dimen.bottom_bar_height_optimal);
        mBottomBarMinHeight = mAppRootView.getResources()
                              .getDimensionPixelSize(R.dimen.bottom_bar_height_min);
        mBottomBarMaxHeight = mAppRootView.getResources()
                              .getDimensionPixelSize(R.dimen.bottom_bar_height_max);
    }

    public void resume() {
        mAppRootView.setLayoutClickEnable(true);
        showModeCoverUntilPreviewReady();
        // showShimmyDelayed();
    }

    private void showShimmyDelayed() {
        // Show shimmy in SHIMMY_DELAY_MS
        mModeListView.showModeSwitcherHint();
    }

    /**
     * A cover view showing the mode theme color and mode icon will be visible
     * on top of preview until preview is ready (i.e. camera preview is started
     * and the first frame has been received).
     */
    private void showModeCoverUntilPreviewReady() {
        int modeId = mAppController.getCurrentModuleIndex();
        int colorId = R.color.mode_cover_default_color;
        int iconId = CameraUtil.getCameraModeCoverIconResId(modeId,
                     mAppController.getAndroidContext());
        mModeTransitionView.setupModeCover(colorId, iconId);
        mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                mModeTransitionView.hideModeCover(null);
                showShimmyDelayed();
            }
        };
        mModeCoverState = COVER_SHOWN;
    }

    public void hideModeCover() {
        if (mModeCoverState == COVER_SHOWN) {
            if (mHideCoverRunnable != null) {
                mAppRootView.post(mHideCoverRunnable);
                mHideCoverRunnable = null;
                mModeCoverState = COVER_HIDDEN;
            }
        }
    }

    @Override
    public void onModeSelected(int modeIndex) {
        Log.e(TAG, "onModeSelected  modeIndex:" + modeIndex);
        // TODO Auto-generated method stub
        mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                mModeListView.startModeSelectionAnimation();
            }
        };
        mModeCoverState = COVER_SHOWN;

        int lastIndex = mAppController.getCurrentModuleIndex();
        // Actual mode teardown / new mode initialization happens here
        mAppController.onModeSelected(modeIndex);
        int currentIndex = mAppController.getCurrentModuleIndex();

        if (lastIndex == currentIndex) {
            hideModeCover();
        }

        updateModeSpecificUIColors();
    }

    @Override
    public int getCurrentModeIndex() {
        // TODO Auto-generated method stub
        return mAppController.getCurrentModuleIndex();
    }

    @Override
    public void onSettingsSelected() {
        // TODO Auto-generated method stub
        mAppController.onSettingsSelected();
    }

    /**
     * Sets a {@link com.westalgo.factorycamera.ui.PreviewStatusListener} that listens
     * to SurfaceTexture changes. In addition, listeners are set on dependent
     * app ui elements.
     *
     * @param previewStatusListener
     *            the listener that gets notified when SurfaceTexture changes
     */
    public void setPreviewStatusListener(
        PreviewStatusListener previewStatusListener) {
        mPreviewStatusListener = previewStatusListener;
        if (mPreviewStatusListener != null) {
            onPreviewListenerChanged();
        }
    }

    /**
     * When the PreviewStatusListener changes, listeners need to be set on the
     * following app ui elements: {@link com.westalgo.factorycamera.ui.PreviewOverlay},
     * {@link com.westalgo.factorycamera.ui.BottomBarWrapper},
     * {@link com.westalgo.factorycamera.ui.IndicatorIconController}.
     */
    private void onPreviewListenerChanged() {
        // Set a listener for recognizing preview gestures.
        GestureDetector.OnGestureListener gestureListener = mPreviewStatusListener
                .getGestureListener();
        if (gestureListener != null) {
            mPreviewOverlay.setGestureListener(gestureListener);
        }
    }

    public void prepareModuleUI() {

        mBottomBarWrapper = (BottomBarModeOptionsWrapper) mAppRootView
                            .findViewById(R.id.indicator_bottombar_wrapper);
        mBottomBarWrapper.setCameraAppUI(this);

        mBottomBar = (BottomBar) mAppRootView.findViewById(R.id.bottom_bar);
        int unpressedColor = mAppController.getAndroidContext().getResources()
                             .getColor(R.color.bottombar_unpressed);
        mBottomBar.setCameraAppUI(this);
        mBottomBar.setShutterButtonEnabled(false);
        mBottomBar.hideThumbnailView();
        setBottomBarColor(unpressedColor);
        updateModeSpecificUIColors();

        mShutterButton = (ShutterButton) mAppRootView
                         .findViewById(R.id.shutter_button);
        addShutterListener(mAppController.getCurrentModuleController());

        mModeOptionsOverlay = (ModeOptionsOverlay) mAppRootView
                              .findViewById(R.id.mode_options_overlay);
        mModeOptionsOverlay.setCameraAppUI(this);
        //switch camera button
        switchButton = (ImageButton) mModeOptionsOverlay.findViewById(R.id.switch_camera);
        switchButton.setOnClickListener(this);
        //only show  switch button in photo mode
        if (mAppController.getCurrentModuleIndex() == ModulesInfo.PHOTO_MODE) {
            switchButton.setVisibility(View.VISIBLE);
        }

        mPreviewOverlay = (PreviewOverlay) mAppRootView
                          .findViewById(R.id.preview_overlay);
        mPreviewOverlay.setOnTouchListener(new MyTouchListener());

        mPreviewOverlay.setOnPreviewTouchedListener(mModeOptionsOverlay);
        addShutterListener(mModeOptionsOverlay);

        // Sets the visibility of the bottom bar and the mode options.
        resetBottomControls(mAppController.getCurrentModuleController(),
                            mAppController.getCurrentModuleIndex());

        // Button Bar
        if (mIndicatorIconController == null) {
            mIndicatorIconController = new IndicatorIconController(
                mAppController, mAppRootView);
        }

        mAppController.getButtonManager().load(mAppRootView);
        mAppController.getButtonManager().setListener(mIndicatorIconController);
        mAppController.getSettingsManager().addListener(
            mIndicatorIconController);
    }

    public void enableSwitchButton(boolean enable) {
        if (enable) {
            switchButton.setVisibility(View.VISIBLE);
        } else {
            switchButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.switch_camera) {
            mAppController.onViewClicked(v);
        }
    }

    /**
     * Remove all the module specific views.
     */
    public void clearModuleUI() {
        removeShutterListener(mAppController.getCurrentModuleController());
    }

    private class MyTouchListener implements View.OnTouchListener {
        private boolean mScaleStarted = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mScaleStarted = false;
            } else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                mScaleStarted = true;
            }

            boolean res = (!mScaleStarted)
                          && mGestureDetector.onTouchEvent(event);
            return res;
        }
    }

    /**
     * This gesture listener finds out the direction of the scroll gestures and
     * sends them to CameraAppUI to do further handling.
     */
    private class MyGestureListener extends
        GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev,
                                float distanceX, float distanceY) {

            if (ev.getEventTime() - ev.getDownTime() > SWIPE_TIME_OUT_MS
                    || mSwipeState != IDLE || !mSwipeEnabled) {
                // Log.e(TAG, "MyGestureListener  onScroll   false");
                return false;
            }

            int deltaX = (int) (ev.getX() - mDown.getX());
            int deltaY = (int) (ev.getY() - mDown.getY());
            if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (Math.abs(deltaX) > mSlop || Math.abs(deltaY) > mSlop) {
                    // Calculate the direction of the swipe.
                    if (deltaX >= Math.abs(deltaY)) {
                        // Swipe right.
                        setSwipeState(SWIPE_RIGHT);
                    }
                }
            }
            // Log.e(TAG, "MyGestureListener  onScroll   true");
            return true;
        }

        private void setSwipeState(int swipeState) {
            // Log.e(TAG, "MyGestureListener  setSwipeState");
            mSwipeState = swipeState;
            // Notify new swipe detected.
            onSwipeDetected(swipeState);
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            // Log.e(TAG, "MyGestureListener  onDown");
            mDown = MotionEvent.obtain(ev);
            mSwipeState = IDLE;
            return false;
        }
    }

    /**
     * Redirects touch events to appropriate recipient views based on swipe
     * direction. More specifically, swipe up and swipe down will be handled by
     * the view that handles mode transition; swipe left will be send to
     * filmstrip; swipe right will be redirected to mode list in order to bring
     * up mode list.
     */
    private void onSwipeDetected(int swipeState) {
        if (swipeState == SWIPE_RIGHT) {
            // Pass the touch to mode switcher
            mAppRootView.redirectTouchEventsTo(mModeListView);
        }
    }

    public void setUIClickEnable(boolean enabled) {
        mAppRootView.setLayoutClickEnable(enabled);
    }

    public void onPreviewAreaChanged(Rect previewRect, float ratio) {
        if (mPreviewRect != previewRect || mAspectRatio != ratio) {
            mPreviewRect = previewRect;
            mAspectRatio = ratio;
            updatePositionConfiguration();
        }
    }

    @Override
    public void onNonDecorWindowSizeChanged(int width, int height, int rotation) {
        // TODO Auto-generated method stub
        if (mWindowWidth != width || mWindowHeight != height) {
            mWindowWidth = width;
            mWindowHeight = height;
            updatePositionConfiguration();
        }
    }

    public void updatePositionConfiguration() {
        int bottomBarHeight = 0;

        if (mAspectRatio == DualTextureViewHelper.MATCH_SCREEN) {
            mBottomBarRect.set(0, mWindowHeight - mBottomBarOptionHeigth,
                               mWindowWidth, mWindowHeight);
            mPreviewRect.set(0, 0, mWindowWidth, mWindowHeight
                             - mBottomBarOptionHeigth);
            if (mAppController.getSettingsManager().getBoolean(
                        SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_USER_SELECTED_ASPECT_RATIO_BACK)) {
                mShouldBottomBarTopOnPreview = true;
            } else {
                mShouldBottomBarTopOnPreview = false;
            }
        } else {
            int previewHeight = mPreviewRect.bottom - mPreviewRect.top;
            if (previewHeight <= 0)
                return;

            // Set weather override priview
            if (mAppController.getSettingsManager().getBoolean(
                        SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_USER_SELECTED_ASPECT_RATIO_BACK)) {
                mShouldBottomBarTopOnPreview = true;
            } else {
                mShouldBottomBarTopOnPreview = false;
            }

            if ((mWindowHeight - previewHeight) >= mBottomBarMinHeight
                    && (mWindowHeight - previewHeight) <= mBottomBarMaxHeight) {
                bottomBarHeight = mWindowHeight - previewHeight;
            } else {
                bottomBarHeight = mBottomBarOptionHeigth;
            }

            if (bottomBarHeight <= 0) {
                bottomBarHeight = mBottomBarOptionHeigth;
            }
            mBottomBarRect.set(0, mWindowHeight - bottomBarHeight,
                               mWindowWidth, mWindowHeight);
        }

        mBottomBar.requestLayout();
        mModeOptionsOverlay.requestLayout();
        mModeListView.requestLayout();
    }

    /**
     * This method should be called in onCameraOpened. It defines CameraAppUI
     * specific changes that depend on the camera or camera settings.
     */
    public void onChangeCamera() {
        ModuleController moduleController = mAppController
                                            .getCurrentModuleController();
        applyModuleSpecs(moduleController.getBottomBarSpec());

        if (mIndicatorIconController != null) {
            // Sync the settings state with the indicator state.
            mIndicatorIconController.syncIndicators();
        }
    }

    /**
     * Applies a {@link com.westalgo.factorycamera.CameraAppUI.BottomBarUISpec} to the
     * bottom bar mode options based on limitations from a
     * {@link com.westalgo.factorycamera.hardware.HardwareSpec}.
     *
     * Options not supported by the hardware are either hidden or disabled,
     * depending on the option.
     *
     * Otherwise, the option is fully enabled and clickable.
     */
    public void applyModuleSpecs(final BottomBarUISpec bottomBarSpec) {
        if (bottomBarSpec == null) {
            return;
        }

        ButtonManager buttonManager = mAppController.getButtonManager();
        buttonManager.setToInitialState();

        // TODO
        /** Front camera mode options */
        CameraInfo info = CameraHolder.instance().getCameraInfo()[CameraHolder.CAMERA_MAIN_ID];
        boolean isFrontCamerasupport = (info.facing == CameraInfo.CAMERA_FACING_FRONT);
        int numberOfCamera = CameraHolder.instance().getNumberOfCameras();
        if (numberOfCamera > 1 && isFrontCamerasupport) {
            if (bottomBarSpec.enableCamera) {
                buttonManager.initializeButton(ButtonManager.BUTTON_CAMERA,
                                               bottomBarSpec.cameraCallback);
            } else {
                buttonManager.disableButton(ButtonManager.BUTTON_CAMERA);
            }
        } else {
            // Hide camera icon if front camera not available.
            buttonManager.hideButton(ButtonManager.BUTTON_CAMERA);
        }

        /** Flash mode options */
        boolean flashBackCamera = mAppController.getSettingsManager()
                                  .getBoolean(SettingsManager.SCOPE_GLOBAL,
                                              Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA);
        if (bottomBarSpec.hideFlash || !flashBackCamera) {
            if (bottomBarSpec.hideFlash) {
                // Hide both flash and torch button in flash disable logic
                buttonManager.hideButton(ButtonManager.BUTTON_FLASH);
            } else {
                if (bottomBarSpec.enableFlash) {
                    buttonManager.initializeButton(ButtonManager.BUTTON_FLASH,
                                                   bottomBarSpec.flashCallback);
                } else {
                    // Hide both flash and torch button in flash disable logic
                    buttonManager.disableButton(ButtonManager.BUTTON_FLASH);
                }
            }
        }

        /** Exposure mode options */
        boolean enableExposureCompensation = bottomBarSpec.enableExposureCompensation
                                             && !(bottomBarSpec.minExposureCompensation == 0
                                                     && bottomBarSpec.maxExposureCompensation == 0 && mAppController
                                                     .getSettingsManager().getBoolean(
                                                             SettingsManager.SCOPE_GLOBAL,
                                                             Keys.KEY_EXPOSURE_COMPENSATION_ENABLED));
        if (enableExposureCompensation) {
            buttonManager.initializePushButton(
                ButtonManager.BUTTON_EXPOSURE_COMPENSATION, null);
            buttonManager.setExposureCompensationParameters(
                bottomBarSpec.minExposureCompensation,
                bottomBarSpec.maxExposureCompensation,
                bottomBarSpec.exposureCompensationStep);

            buttonManager
            .setExposureCompensationCallback(bottomBarSpec.exposureCompensationSetCallback);
            buttonManager.updateExposureButtons();
        } else {
            buttonManager
            .hideButton(ButtonManager.BUTTON_EXPOSURE_COMPENSATION);
            buttonManager.setExposureCompensationCallback(null);
        }

        //ISO setting
        boolean enableISOSetting = bottomBarSpec.enableISOSettings
                && mAppController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL,
                        Keys.KEY_ISO_ENABLED);
        if (enableISOSetting) {
            buttonManager.initializePushButton(ButtonManager.BUTTON_ISO, null);
            buttonManager.setISOSettingCallback(bottomBarSpec.isoSetCallback);
            buttonManager.updateISOSettings();
        } else {
            buttonManager.hideButton(ButtonManager.BUTTON_ISO);
            buttonManager.setISOSettingCallback(null);
        }
    }

    /*************************** Mode options api *****************************/

    public void enableModeOptions() {
        mModeOptionsOverlay.setToggleClickable(true);
    }

    /**
     * Set the mode options visible.
     */
    public void showModeOptions() {
        /* Make mode options clickable. */
        enableModeOptions();
        mModeOptionsOverlay.setVisibility(View.VISIBLE);
    }

    /**
     * Set the mode options invisible. This is necessary for modes that don't
     * show a bottom bar for the capture UI.
     */
    public void hideModeOptions() {
        mModeOptionsOverlay.setVisibility(View.INVISIBLE);
    }

    /**************************** Bottom bar api ******************************/

    public void gotoGallery() {
        mAppController.gotoGallery();
    }

    public Rect getPreviewRect() {
        return mPreviewRect;
    }

    public Rect getBottomBarRect() {
        return mBottomBarRect;
    }

    public boolean shouldOverlayBottomBar() {
        // TODO Auto-generated method stub
        return mShouldBottomBarTopOnPreview;
    }

    /**
     * Sets up the bottom bar and mode options with the correct shutter button
     * and visibility based on the current module.
     */
    public void resetBottomControls(ModuleController module, int moduleIndex) {
        showBottomBar();
        showModeOptions();
        setBottomBarShutterIcon(moduleIndex);
    }

    public void setThumbnailResource(Bitmap bitmap) {
        mBottomBar.setThumbnailResource(bitmap);
    }

    public void enableThumbnai(boolean isEnable) {
        mBottomBar.enableThumbnail(isEnable);
    }

    public void showProgressView(boolean isShow) {
        mModeOptionsOverlay.showProgressView(isShow);
    }

    /**
     * Set the bottom bar visible.
     */
    public void showBottomBar() {
        mBottomBar.setVisibility(View.VISIBLE);
    }

    /**
     * Set the bottom bar invisible.
     */
    public void hideBottomBar() {
        mBottomBar.setVisibility(View.INVISIBLE);
    }

    private void updateModeSpecificUIColors() {
        setBottomBarColorsForModeIndex(mAppController.getCurrentModuleIndex());
    }

    /**
     * Sets the color of the bottom bar.
     */
    public void setBottomBarColor(int colorId) {
        mBottomBar.setBackgroundColor(colorId);
    }

    /**
     * Sets the pressed color of the bottom bar for a camera mode index.
     */
    public void setBottomBarColorsForModeIndex(int index) {
        mBottomBar.setColorsForModeIndex(index);
    }

    /**
     * Sets the shutter button icon on the bottom bar, based on the mode index.
     */
    public void setBottomBarShutterIcon(int modeIndex) {
        int shutterIconId = CameraUtil.getCameraShutterIconId(modeIndex,
                            mAppController.getAndroidContext());
        mBottomBar.setShutterButtonIcon(shutterIconId);
    }

    /**
     * Add a {@link #ShutterButton.OnShutterButtonListener} to the shutter button.
     */
    public void addShutterListener(
        ShutterButton.OnShutterButtonListener listener) {
        mShutterButton.addOnShutterButtonListener(listener);
    }

    /**
     * Remove a {@link #ShutterButton.OnShutterButtonListener} from the shutter button.
     */
    public void removeShutterListener(
        ShutterButton.OnShutterButtonListener listener) {
        mShutterButton.removeOnShutterButtonListener(listener);
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        mBottomBar.post(new Runnable() {
            @Override
            public void run() {
                mBottomBar.setShutterButtonEnabled(enabled);
            }
        });
    }

    public boolean isShutterButtonEnabled() {
        return mBottomBar.isShutterButtonEnabled();
    }

    public interface AnimationFinishedListener {
        public void onAnimationFinished(boolean success);
    }

    /**
     * BottomBarUISpec provides a structure for modules to specify their ideal
     * bottom bar mode options layout.
     *
     * Once constructed by a module, this class should be treated as read only.
     *
     * The application then edits this spec according to hardware limitations
     * and displays the final bottom bar ui.
     */
    public static class BottomBarUISpec {
        /** Mode options UI */

        /**
         * Set true if the camera option should be enabled. If not set or false,
         * and multiple cameras are supported, the camera option will be
         * disabled.
         *
         * If multiple cameras are not supported, this preference is ignored and
         * the camera option will not be visible.
         */
        public boolean enableCamera;

        /**
         * Set true if the camera option should not be visible, regardless of
         * hardware limitations.
         */
        public boolean hideCamera;

        /**
         * Set true if the photo flash option should be enabled. If not set or
         * false, the photo flash option will be disabled.
         *
         * If the hardware does not support multiple flash values, this
         * preference is ignored and the flash option will be disabled. It will
         * not be made invisible in order to preserve a consistent experience
         * across devices and between front and back cameras.
         */
        public boolean enableFlash;

        /**
         * Set true if flash should not be visible, regardless of hardware
         * limitations.
         */
        public boolean hideFlash;

        public boolean enableExposureCompensation;

        public boolean enableISOSettings;

        /** Mode options callbacks */

        /**
         * A {@link com.westalgo.factorycamera.ButtonManager.ButtonCallback} that will
         * be executed when the camera option is pressed. This callback can be
         * null.
         */
        public ButtonManager.ButtonCallback cameraCallback;

        /**
         * A {@link com.westalgo.factorycamera.ButtonManager.ButtonCallback} that will
         * be executed when the flash option is pressed. This callback can be
         * null.
         */
        public ButtonManager.ButtonCallback flashCallback;

        /**
         * A ExposureCompensationSetCallback that will execute when an expsosure
         * button is pressed. This callback can be null.
         */
        public interface ExposureCompensationSetCallback {
            public void setExposure(int value);
        }

        public ExposureCompensationSetCallback exposureCompensationSetCallback;

        //ISO setting callback
        public interface ISOSetCallback {
            public void setISOValue(String value);
        }
        public ISOSetCallback isoSetCallback;

        /**
         * Exposure compensation parameters.
         */
        public int minExposureCompensation;
        public int maxExposureCompensation;
        public float exposureCompensationStep;
    }
}
