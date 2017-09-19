package com.westalgo.factorycamera.settings;


import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

public class ProjectConfig {

    private static final String TAG = ProjectConfig.class.getSimpleName();

    public static final int IMAGE_FORMAT = ImageFormat.JPEG;
    public static final boolean useHal1 = true;

    /**
     *config current project here
     */
    private final String currentProject = HP55C71;

    /**
     *project name must match with xml file name
     *below are list of all project
     */
    private static final String DEFAULT_PROJECT = "project_default.xml";
    private static final String EASTAEON_F620 = "project_eastaeon_f620.xml";
    private static final String XIAOWA = "project_xiaowa.xml";
    private static final String HP55C71 = "project_hp55c71.xml";
    private static final String VSUN_V5507B = "project_vsun_v5507b.xml";

    /**
     * if set true, only show verify mode for app
     */
    public static final boolean VERIFY_MODE_ONLY = false;

    /**
     *dual camera module direction,
     * the tag 'dualcam_direction' in xml file must use below value
     */
    private final int MAIN_UP_AUX_DOWN = 0;
    private final int MAIN_DOWN_AUX_UP = 1;
    private final int MAIN_LEFT_AUX_RIGHT = 2;
    private final int MAIN_RIGHT_AUX_LEFT = 3;

    private static ProjectConfig mConfig;
    /**
     *All the variable that need read from xml file
     */
    private String mProjectName;
    private String mPlatform;
    private int mainId;
    private int auxId;
    private int dualcamDirection;
    private boolean isFixedRotation;
    private int fixedRotation;
    private int dynamicRotation;
    private String extraParameters;


    public static void init(Context context) {
        if (mConfig == null) {
            mConfig = new ProjectConfig(context);
        }
    }

    public static ProjectConfig getConfig() {
        return mConfig;
    }

    private ProjectConfig(Context context) {
        loading(context,currentProject);
        dumpInfo();
    }

    public int getMainId() {
        return mainId;
    }

    public int getAuxId() {
        return auxId;
    }

    public String getProjectName() {
        return mProjectName;
    }

    /**we use jpegRotation not device orientation
     * detail see Camera.Parameters.setRotation() in android developer
     */
    public int getFinalRotation(int jpegRotation) {
        //rotation not effect by sensor
        if (isFixedRotation) {
            return fixedRotation;
        } else {
            //use manual rotation setting
            if (dynamicRotation != -1) {
                return (jpegRotation + dynamicRotation) % 360;
            } else {
                //calculate rotation by dual camera direction
                return calcRotationByDirection(dualcamDirection, jpegRotation);
            }
        }
    }

    /**
     *
     * set extra parameters ,key-value read from tag 'extra_parameters' in xml file
     */
    public void setExtraParameters(Camera.Parameters parameters) {
        setExtraParameters(extraParameters, parameters);
    }

    private int calcRotationByDirection(int direction, int jpegRotation) {
        int finalRotation;
        switch (direction) {
            case MAIN_UP_AUX_DOWN:
                finalRotation = jpegRotation;
                break;
            case MAIN_DOWN_AUX_UP:
                finalRotation = (jpegRotation + 180) % 360;
                break;
            case MAIN_LEFT_AUX_RIGHT:
                finalRotation = (jpegRotation + 270) % 360;
                break;
            case MAIN_RIGHT_AUX_LEFT:
                finalRotation = (jpegRotation + 90) % 360;
                break;
            default:
                finalRotation = -1;
                Log.e(TAG, "dual cam direction config error");
                break;
        }
        return finalRotation;
    }

    /**
     * read data from xml file in assets folder
     */
    private void loading(final Context context, final String project) {

        try {
            int eventType ;
            InputStream in = context.getResources().getAssets().open(project);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xrp = factory.newPullParser();
            xrp.setInput(in,"UTF-8");
            eventType = xrp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG: {
                        String tagName = xrp.getName();
                        if (TextUtils.isEmpty(tagName)) {
                            Log.d(TAG, "tagName is null!!!");
                            return;
                        }
                        if ("config".equals(tagName)) {
                            getValueFromXml(xrp);
                        }
                    }
                    break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    default:
                        break;
                }
                eventType = xrp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }

    private void getValueFromXml(XmlPullParser xrp) {
        mProjectName = xrp.getAttributeValue(null, "project");
        mPlatform = xrp.getAttributeValue(null, "platform");
        dualcamDirection = Integer.parseInt(xrp.getAttributeValue(null, "dualcam_direction"));
        mainId = Integer.parseInt(xrp.getAttributeValue(null, "main_id"));
        auxId = Integer.parseInt(xrp.getAttributeValue(null, "aux_id"));
        isFixedRotation = Boolean.parseBoolean(xrp.getAttributeValue(null, "is_fixed_rotation"));
        fixedRotation = Integer.parseInt(xrp.getAttributeValue(null, "fixed_rotation"));
        dynamicRotation = Integer.parseInt(xrp.getAttributeValue(null, "dynamic_rotation"));
        extraParameters = xrp.getAttributeValue(null, "extra_parameters");
    }

    private void setExtraParameters(String str, Camera.Parameters parameters) {
        if (TextUtils.isEmpty(str) || str == null) {
            Log.w(TAG,"no extra parameters found");
            return;
        }
        String[] result = str.split(",");
        if (result.length % 2 != 0) {
            Log.e(TAG,"extra parameters num error");
            return;
        }
        for (int i = 0;i<result.length/2;i++) {
            parameters.set(result[2 * i], result[2 * i + 1]);
        }

    }

    private void dumpInfo() {
        Log.d(TAG, "project:" + mProjectName + "\n"
                + "platform:" + mPlatform + "\n"
                + "dualcamDirection:" + dualcamDirection + "\n"
                + "mainId:" + mainId + "\n"
                + "auxId:" + auxId + "\n"
                + "isFixedRotation:" + isFixedRotation + "\n"
                + "fixedRotation:" + fixedRotation + "\n"
                + "dynamicRotation:" + dynamicRotation + "\n"
                + "extraParameters:" + extraParameters + "\n");
    }
}