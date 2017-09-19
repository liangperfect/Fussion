package com.westalgo.factorycamera.debug;

public class Log {
    /**
     * All DCS logging using this class will use this tag prefix. Additionally,
     * the prefix itself is checked in isLoggable and serves as an override. So,
     * to toggle all logs allowed by the current {@link Configuration}, you can
     * set properties:
     *
     * adb shell setprop log.tag.DCS_ VERBOSE adb shell setprop log.tag.DCS_ ""
     */
    public static final String DCS_LOGTAG_PREFIX = "DCS_";
    private static final Log.Tag TAG = new Log.Tag("Log");

    /**
     * This class restricts the length of the log tag to be less than the
     * framework limit and also prepends the common tag prefix defined by
     * {@code DCS_LOGTAG_PREFIX}.
     */
    public static final class Tag {

        // The length limit from Android framework is 23.
        private static final int MAX_TAG_LEN = 23 - DCS_LOGTAG_PREFIX.length();

        final String mValue;

        public Tag(String tag) {
            final int lenDiff = tag.length() - MAX_TAG_LEN;
            if (lenDiff > 0) {
                w(TAG, "Tag " + tag + " is " + lenDiff
                  + " chars longer than limit.");
            }
            mValue = DCS_LOGTAG_PREFIX
                     + (lenDiff > 0 ? tag.substring(0, MAX_TAG_LEN) : tag);
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    public static void d(Tag tag, String msg) {
        if (isLoggable(tag, android.util.Log.DEBUG)) {
            android.util.Log.d(tag.toString(), msg);
        }
    }

    public static void e(Tag tag, String msg) {
        if (isLoggable(tag, android.util.Log.ERROR)) {
            android.util.Log.e(tag.toString(), msg);
        }
    }

    public static void e(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, android.util.Log.ERROR)) {
            android.util.Log.e(tag.toString(), msg, tr);
        }
    }

    public static void i(Tag tag, String msg) {
        if (isLoggable(tag, android.util.Log.INFO)) {
            android.util.Log.i(tag.toString(), msg);
        }
    }

    public static void i(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, android.util.Log.INFO)) {
            android.util.Log.i(tag.toString(), msg, tr);
        }
    }

    public static void v(Tag tag, String msg) {
        if (isLoggable(tag, android.util.Log.VERBOSE)) {
            android.util.Log.v(tag.toString(), msg);
        }
    }

    public static void w(Tag tag, String msg) {
        if (isLoggable(tag, android.util.Log.WARN)) {
            android.util.Log.w(tag.toString(), msg);
        }
    }

    public static void w(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, android.util.Log.WARN)) {
            android.util.Log.w(tag.toString(), msg, tr);
        }
    }

    private static boolean isLoggable(Tag tag, int level) {
        try {
            boolean ret = true;
            if (showLogLevel() >= level) {
                return ret;
            }
            if (showLogLevel() == 0 && level == android.util.Log.ERROR) {
                return ret;
            }
            return false;
        } catch (IllegalArgumentException ex) {
            e(TAG, "Tag too long:" + tag);
            return false;
        }
    }

    /*
     * ERROR_ONLY = 0 VERBOSE = 2 DEBUG = 3 INFO = 4 WARN = 5 ERROR = 6 return 0
     * indicates show only show error log return 2 indicates show verbose log
     * return 3 indicates show VERBOSE DEBUG log return 4 indicates show VERBOSE
     * DEBUG INFO log return 5 indicates show VERBOSE DEBUG INFO WARN log return
     * 6 indicates show all logs
     */
    private static int showLogLevel() {
        return 6;
    }
}
