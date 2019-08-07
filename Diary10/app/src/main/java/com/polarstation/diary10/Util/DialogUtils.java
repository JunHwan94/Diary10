package com.polarstation.diary10.Util;

import android.app.ProgressDialog;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.ViewGroup;
import android.view.WindowManager;

public class DialogUtils {
    public static void showProgressDialog(ProgressDialog progressDialog, String stringResource){
        SpannableString message = new SpannableString(stringResource);
        message.setSpan(new RelativeSizeSpan(1.1f), 0, message.length(), 0);
        progressDialog.setMessage(message);
        progressDialog.show();

        WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
        params.width = 1100;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        progressDialog.getWindow().setAttributes(params);
    }
}
