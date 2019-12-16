package com.polarstation.diary10.fragment

import android.app.Activity

interface DiaryFragmentCallback{
    fun loadDiary(key: String?)
    fun finishDiaryActivity()
    fun getActivity(): Activity
    fun dataChanges()
}