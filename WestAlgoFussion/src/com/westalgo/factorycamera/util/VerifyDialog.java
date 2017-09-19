package com.westalgo.factorycamera.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.view.ContextThemeWrapper;

import com.westalgo.factorycamera.R;


public class VerifyDialog implements DialogInterface.OnClickListener {


    private String mTitle;
    private String mMessage;

    private int verifyResult;
    private final int RED_THEME = R.style.AlertDialogCustomRed;
    private final int GREEN_THEME = R.style.AlertDialogCustomGreen;
    private final int TOTAL_TIME_LONG = 10000;
    private final int TOTAL_TIME_SHORT = 2000;
    private final int TIME_TICK = 1000;


    private AlertDialog dialog;
    private CountDownTimer mTimer;
    private Listener mListener;

    public interface Listener {
        void onFinished(boolean success);
    }

    public VerifyDialog(int result, Context context) {
        verifyResult = result;
        initDialogMessage(context);
        createDialog(context);
    }

    private void createDialog(Context context) {
        int theme = verifyResult <= 0 ? GREEN_THEME : RED_THEME;
        AlertDialog.Builder builder =
                new AlertDialog.Builder(new ContextThemeWrapper(context, theme));
        builder.setTitle(mTitle);
        builder.setMessage(mMessage);
        builder.setCancelable(false);
        if (verifyResult > 0) {
            builder.setPositiveButton(R.string.dialog_done, this);
            builder.setMessage(null);
        }
        dialog = builder.create();
    }

    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        if (mListener != null) {
            mListener.onFinished(false);
        }
    }

    private void createCountDownTimer() {
        int totalTime = verifyResult == -1 ? TOTAL_TIME_SHORT : TOTAL_TIME_LONG;
        mTimer = new CountDownTimer(totalTime, TIME_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                dialog.setMessage(mMessage + millisUntilFinished / TIME_TICK);
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
                mTimer.cancel();
                if (mListener != null) {
                    mListener.onFinished(true);
                }
            }
        };
    }

    private void initDialogMessage(Context context) {
        mMessage = context.getResources().getString(R.string.verify_write_epprom);
        if (verifyResult <= 0) {
            mTitle = context.getResources().getString(R.string.verify_back_result) + verifyResult;
            createCountDownTimer();
        } else {
            mTitle = String.format(
                    context.getResources().getString(R.string.fail_to_verify), verifyResult);
        }
    }

    public void showDialog(Listener listener) {
        mListener = listener;
        dialog.show();
        if (verifyResult <= 0) {
            mTimer.start();
        }
    }
}
