package com.polarstation.diary10.fragment;

import android.app.Activity;

public interface DiaryFragmentCallback {
    void loadDiary(String key);
    void finishDiaryActivity();
    Activity getActivity();
    void dataChanges();
//    void movePageToCover(int idx);
}
