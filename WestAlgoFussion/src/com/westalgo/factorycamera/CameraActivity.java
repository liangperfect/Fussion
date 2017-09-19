package com.westalgo.factorycamera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import android.widget.Toast;

import com.westalgo.factorycamera.ButtonManager;
import com.westalgo.factorycamera.MediaSaveService;
import com.westalgo.factorycamera.R;
import com.westalgo.factorycamera.app.AppController;
import com.westalgo.factorycamera.app.CameraApp;
import com.westalgo.factorycamera.app.CameraAppUI;
import com.westalgo.factorycamera.debug.Log;
import com.westalgo.factorycamera.manager.CameraManager;
import com.westalgo.factorycamera.manager.CameraManager.CameraOpenErrorCallback;
import com.westalgo.factorycamera.module.ModuleController;
import com.westalgo.factorycamera.module.ModuleManagerImpl;
import com.westalgo.factorycamera.module.ModulesInfo;
import com.westalgo.factorycamera.settings.CameraSettingsActivity;
import com.westalgo.factorycamera.settings.Keys;
import com.westalgo.factorycamera.settings.SettingsManager;
import com.westalgo.factorycamera.ui.MainActivityLayout;
import com.westalgo.factorycamera.ui.ModeListView;
import com.westalgo.factorycamera.util.BitmapUtils;
import com.westalgo.factorycamera.util.CameraUtil;
import com.westalgo.factorycamera.util.VerifyDialog;
import com.westalgo.factorycamera.manager.CameraHolder;



public class CameraActivity extends Activity implements AppController, Callback {

    private static final Log.Tag TAG = new Log.Tag("CameraActivity");

    public static final String MODULE_SCOPE_PREFIX = "_preferences_module_";
    public static final String CAMERA_SCOPE_PREFIX = "_preferences_camera_";
    public static final int PREVIEW_DOWN_SAMPLE_FACTOR = 5;
    private static final String GALLERY_PACKAGE_NAME = "com.android.gallery3d";
    private static final String GALLERY_ACTIVITY_CLASS = "com.android.gallery3d.app.GalleryActivity";
    private static final int MSG_UPDATE_THUMBNAIL = 1;
    private static final int CAMERA_MODE_DUALCAMERA_INDEX = 1;
    private final int VERIFY_DIALOG_HITE_TIME = 10000;
    private final int TIME_TICK = 1000;

    private CameraAppUI mCameraAppUI;
    private ModeListView mModeListView;
    private ModuleManagerImpl mModuleManager;
    private Context mContext;
    private SettingsManager mSettingsManager;

    private ModuleController mCurrentModule;
    private int mCurrentModeIndex = -1;

    private MainActivityLayout mCameraViewRoot;

    private ButtonManager mButtonManager;
    private int mLastRawOrientation;
    private  MyOrientationEventListener mOrientationListener = null;

    private FrameLayout mTextureViewGroup;
    // for thumbnail
    private Bitmap mThumbnailBitmap = null;
    private  String mOrderClause;
    private  Uri mBaseUri;
    private  ContentResolver mResolver;

    //result show
    private VerifyResultListAdapter mResultListAdapter;
    private FrameLayout verifyResultFrameL;
    private ListView verifyListView;
    private Button nextButton;
/*    AlertDialog showResultDialog;
    View verify_dialog_view;
    TextView verify_dialog_tv;
    Button verify_dialog_btn;
    AlertDialog verify_dialog;*/
    private Uri currentImgUri;

    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //set default value
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.camera_preferences, false);
        setContentView(R.layout.camera);
        mContext = getApplication().getBaseContext();
        CameraUtil.initialize(mContext);


        mHandler = new Handler(this);
        mCameraViewRoot = (MainActivityLayout)findViewById(R.id.camera_app_root);
        mTextureViewGroup = (FrameLayout) findViewById(R.id.texture_view_root);
        mCameraAppUI = new CameraAppUI(this, mCameraViewRoot);
//        mSettingsManager = new SettingsManager(mContext);
        mSettingsManager = ((CameraApp)(this.getApplication())).getSettingsManager();
        Keys.setDefaults(mSettingsManager, mContext);

        mModuleManager = new ModuleManagerImpl();
        ModulesInfo.setupModules(this, mModuleManager);

        mModeListView = (ModeListView) findViewById(R.id.mode_list_layout);
        mModeListView.init(mModuleManager.getSupportedModeIndexList());
        mModeListView.requestLayout();

        setModuleFromModeIndex(getModeIndex());
        if (getModeIndex() == CAMERA_MODE_DUALCAMERA_INDEX) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mCameraAppUI.prepareModuleUI();

        // mCameraAppUI.
        mCurrentModule.init(this, mCameraViewRoot);

        //verify result UI
        verifyResultFrameL = (FrameLayout) findViewById(R.id.verify_result_framelayout);
        verifyResultFrameL.setVisibility(View.GONE);
        verifyListView = (ListView) findViewById(R.id.verify_result_list);
        nextButton = (Button) findViewById(R.id.verify_next_btn);
        //verify result show adapter
        mResultListAdapter = new VerifyResultListAdapter(mContext, null);
        Log.i(TAG, "set results show adapter.....");
        verifyListView.setAdapter(mResultListAdapter);

        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "next button clicked, finish.");
                finish();
            }
        });

        mOrientationListener = new MyOrientationEventListener(this);

        mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        mOrderClause = ImageColumns.DATE_TAKEN + " DESC, " + ImageColumns._ID + " DESC";
        mResolver = this.getContentResolver();

    }

    private MediaSaveService mMediaSaveService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder b) {
            mMediaSaveService = ((MediaSaveService.LocalBinder) b).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (mMediaSaveService != null) {
                mMediaSaveService.setListener(null);
                mMediaSaveService = null;
            }
        }
    };
    @Override
    public MediaSaveService getMediaSaveService() {
        return mMediaSaveService;
    }

    private void bindMediaSaveService() {
        Intent intent = new Intent(this, MediaSaveService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMediaSaveService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub

        //luke enable meida save service when is debug mode
        if (CameraUtil.DEBUG_MODE) {
            bindMediaSaveService();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (getCurrentModuleIndex() == CAMERA_MODE_DUALCAMERA_INDEX) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        mCameraAppUI.resume();
        mCurrentModule.resume();
        mOrientationListener.enable();
        enableAllUI();
        updateCurrentThumbnail();
    }

    public void updateCurrentThumbnail() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                mThumbnailBitmap = BitmapUtils.getThumbFromDatabase(CameraActivity.this);
                if (mThumbnailBitmap == null) {
                    currentImgUri = null;
                } else {
                    mHandler.sendEmptyMessage(MSG_UPDATE_THUMBNAIL);
                }
            }
        }).start();
    }

    public void updateCurrentImgUri(Uri uri) {
        currentImgUri = uri;
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mCurrentModule.pause();
        mOrientationListener.disable();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        //luke enable meida save service when is debug mode
        if (CameraUtil.DEBUG_MODE) {
            unbindMediaSaveService();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mCurrentModule.destroy();
    }

    public void enableAllUI() {
        showProgressView(false);
        setUIClickEnable(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_HOME == keyCode) {
        } else if (KeyEvent.KEYCODE_BACK == keyCode) {
        }
        return super.onKeyDown(keyCode, event);
    }

    public void hideModeCover() {
        mCameraAppUI.hideModeCover();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        if (mModeListView != null) {
            mModeListView.setVisibility(View.VISIBLE);
            mModeListView.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public Context getAndroidContext() {
        // TODO Auto-generated method stub
        return mContext;
    }

    @Override
    public void onModeSelected(int moduleIndex) {
        int lastIndex = mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED);
        if (moduleIndex == lastIndex) {
            Log.d(TAG, "Camera mode not changed, do nothing.");
            return;
        }
        mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, moduleIndex);
        //only show  switch button in photo mode
        mCameraAppUI.enableSwitchButton(moduleIndex == ModulesInfo.PHOTO_MODE);
        //release main and sub when switch mode.
        closeModule(mCurrentModule);
        setModuleFromModeIndex(moduleIndex);
        // Eirot add for DualCamera mode start
        if (moduleIndex == CAMERA_MODE_DUALCAMERA_INDEX) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        Log.d(TAG, "--->> onModeSelected: screenOrientation = " + this.getResources().getConfiguration().orientation);
        // Eirot add for DualCamera mode end

        mCameraAppUI.addShutterListener(mCurrentModule);
        openModule(mCurrentModule);

        mCurrentModule.onOrientationChanged(mLastRawOrientation);
    }

    @Override
    public void onSettingsSelected() {
        // TODO Auto-generated method stub
        Intent intent = new Intent(this, CameraSettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public SettingsManager getSettingsManager() {
        return mSettingsManager;
    }

    @Override
    public int getCurrentModuleIndex() {
        return mCurrentModeIndex;
    }

    @Override
    public ModuleController getCurrentModuleController() {
        return mCurrentModule;
    }

    public CameraOpenErrorCallback getCameraOpenErrorCallback() {
        return mCameraOpenErrorCallback;
    }

    @Override
    public CameraAppUI getCameraAppUI() {
        return mCameraAppUI;
    }

    public void setUIClickEnable(boolean enabled) {
        mCameraAppUI.setUIClickEnable(enabled);
    }

    /**
     * Get the current mode index from the Intent or from persistent
     * settings.
     */
    public int getModeIndex() {
        int modeIndex = mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED);
        if(modeIndex < 0) {
            modeIndex = 0;
        }
        return modeIndex;
    }

    /**
     * Sets the mCurrentModuleIndex, creates a new module instance for the given
     * index an sets it as mCurrentModule.
     */
    private void setModuleFromModeIndex(int modeIndex) {
        ModuleManagerImpl.ModuleAgent agent = mModuleManager.getModuleAgent(modeIndex);
        if (agent == null) {
            return;
        }
        mCurrentModeIndex = agent.getModuleId();
        mCurrentModule = (ModuleController) agent.createModule();
    }

    private void openModule(ModuleController module) {
        module.init(this, mCameraViewRoot);
        module.resume();
    }

    private void closeModule(ModuleController module) {
        module.pause();
        mCameraAppUI.clearModuleUI();
    }

    public void onPreviewAreaChanged(Rect previewRect, float ratio) {
        mCameraAppUI.onPreviewAreaChanged(previewRect, ratio);
    }

    public void updateThumbnail() {
        mCameraAppUI.setThumbnailResource(mThumbnailBitmap);
    }

    public void saveThumbnail() {
        enableThumbnai(false);
        mThumbnailBitmap = mCurrentModule.getPreviewBitmap(PREVIEW_DOWN_SAMPLE_FACTOR);
        new Thread(new Runnable() {

            @Override
            public void run() {
                mThumbnailBitmap = BitmapUtils.resizeAndCropCenter(mThumbnailBitmap, BitmapUtils.MicrothumbnailTargetSize, true);
            }
        }).start();
    }

    public void saveAndUpdateThumbnail() {
        enableThumbnai(false);
        mThumbnailBitmap = mCurrentModule.getPreviewBitmap(PREVIEW_DOWN_SAMPLE_FACTOR);
        new Thread(new Runnable() {

            @Override
            public void run() {
                mThumbnailBitmap = BitmapUtils.resizeAndCropCenter(mThumbnailBitmap, BitmapUtils.MicrothumbnailTargetSize, true);
                mHandler.sendEmptyMessage(MSG_UPDATE_THUMBNAIL);
            }
        }).start();
    }

    public void enableThumbnai(boolean isEnable) {
        mCameraAppUI.enableThumbnai(isEnable);
    }

    public void showProgressView(boolean isShow) {
        mCameraAppUI.showProgressView(isShow);
    }

    public  Intent getGalleryIntent(Context context) {
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(currentImgUri, "image/jpeg");
    }

    @Override
    public void gotoGallery() {
        if (currentImgUri == null) {
            Toast.makeText(this," image has been delete",Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(getGalleryIntent(this));
    }

    /*********************************Set Camera State*******************************************/

    private CameraOpenErrorCallback mCameraOpenErrorCallback = new CameraOpenErrorCallback() {
        @Override
        public void onCameraDisabled(int cameraId) {

            CameraUtil.showErrorAndFinish(CameraActivity.this,
                                          R.string.camera_disabled);
        }

        @Override
        public void onDeviceOpenFailure(int cameraId) {

            CameraUtil.showErrorAndFinish(CameraActivity.this,
                                          R.string.cannot_connect_camera);
        }

        @Override
        public void onReconnectionFailure(CameraManager mgr) {

            CameraUtil.showErrorAndFinish(CameraActivity.this,
                                          R.string.cannot_connect_camera);
        }
    };

    @Override
    public void onViewClicked(View v){
        if (v.getId() == R.id.switch_camera) {
            getCurrentModuleController().pause();
            CameraHolder.instance().setNextSingleCameraId();
            getCurrentModuleController().resume();
        }
    }

    @Override
    public String getCameraScope() {
        int currentCameraId = 1;
        if (currentCameraId < 0) {
            // if an unopen camera i.e. negative ID is returned, which we've observed in
            // some automated scenarios, just return it as a valid separate scope
            // this could cause user issues, so log a stack trace noting the call path
            // which resulted in this scenario.
            Log.w(TAG, "getting camera scope with no open camera, using id: " + currentCameraId);
        }
        return CAMERA_SCOPE_PREFIX + Integer.toString(currentCameraId);
    }

    @Override
    public String getModuleScope() {
        return MODULE_SCOPE_PREFIX + mCurrentModule.getModuleStringIdentifier();
    }

    @Override
    public ButtonManager getButtonManager() {
        if (mButtonManager == null) {
            mButtonManager = new ButtonManager(this);
        }
        return mButtonManager;
    }

    public void setShutterEnabled(boolean enabled) {
        mCameraAppUI.setShutterButtonEnabled(enabled);
    }

    public boolean isShutterEnabled() {
        return mCameraAppUI.isShutterButtonEnabled();
    }


    // This listens to the device orientation, so we can update the compensation.
    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) {
                return;
            }

            final int roundedOrientation = CameraUtil.roundOrientation(orientation, 0);
            if (roundedOrientation != mLastRawOrientation) {
                Log.v(TAG, "orientation changed (from:to) " + mLastRawOrientation +
                      ":" + roundedOrientation);
                mLastRawOrientation = roundedOrientation;
                if (mCurrentModule != null) {
                    mCurrentModule.onOrientationChanged(roundedOrientation);
                }
            }
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_UPDATE_THUMBNAIL:
            updateThumbnail();
            break;
        }
        return true;
    }
    public void updateResult(float[] results) {
        if (results != null) {
            Log.i(TAG, "update verify results.....");
            mResultListAdapter.refresh(results);
        }
    }


   public void showVerifyDialog(int result) {
       VerifyDialog dialog = new VerifyDialog(result, this);
       dialog.showDialog(new VerifyDialog.Listener() {
           @Override
           public void onFinished(boolean success) {
               if (success) {
                   String msg = getResources().getString(R.string.verify_finished);
                   Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
               }
               finish();
           }
       });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
    }

}
