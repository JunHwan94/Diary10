package com.polarstation.diary10.fragment;

import android.app.Activity;

public interface PageFragmentCallback {
    void loadDiary(String key);
    void finishDiaryActivity();
    Activity getActivity();
    void dataChanges();
}
