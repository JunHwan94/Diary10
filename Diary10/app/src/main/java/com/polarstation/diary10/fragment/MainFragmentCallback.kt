package com.polarstation.diary10.fragment

import android.app.Activity
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter

interface MainFragmentCallback{
    fun replaceFragment(type: Int)
    fun quitApp()
    fun getActivity(): Activity
    fun setNavigationViewDisabled()
    fun notifyAdapter(adapter: DiaryRecyclerViewAdapter)
}