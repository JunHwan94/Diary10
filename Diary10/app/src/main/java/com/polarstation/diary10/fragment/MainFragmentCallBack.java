package com.polarstation.diary10.fragment;

import android.app.Activity;

public interface MainFragmentCallBack {
    void findMyDiary();
    void replaceFragment(int type);
    void quitApp();
    void cancelDialog();
    Activity getActivity();
}
