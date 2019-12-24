package com.polarstation.diary10.fragment

import android.app.Activity
import com.polarstation.diary10.activity.DiaryActivity

interface DiaryFragmentCallback{
    fun loadDiary(key: String?, pagerAdapter: DiaryActivity.ListPagerAdapter)
    fun finishDiaryActivity()
    fun getActivity(): Activity
    fun dataChanges()
}