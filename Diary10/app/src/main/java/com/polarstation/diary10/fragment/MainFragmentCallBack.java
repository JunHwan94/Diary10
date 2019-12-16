package com.polarstation.diary10.fragment;

import android.app.Activity;

import com.polarstation.diary10.data.DiaryRecyclerViewAdapter;

public interface MainFragmentCallBack {
    void replaceFragment(int type);
    void quitApp();
    Activity getActivity();
    void setNavigationViewDisabled();
    void notifyAdapter(DiaryRecyclerViewAdapter adapter);
}
