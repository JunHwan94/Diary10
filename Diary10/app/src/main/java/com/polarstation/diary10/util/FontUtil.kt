package com.polarstation.diary10.util

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import java.util.*

const val PATH = "fonts/JalnanOTF.otf"
open class FontUtil{
    companion object{
        private var typefaceOp = Optional.empty<Typeface>()
        fun setGlobalFont(view: View?) {
            if(!typefaceOp.isPresent) typefaceOp = Optional.of(Typeface.createFromAsset(view!!.context.assets, PATH))
            if(view != null){
                if(view is ViewGroup){
                    view.forEach {
                        if(it is TextView) it.typeface = typefaceOp.get()
                        setGlobalFont(it)
                    }
                }
            }
        }
    }
}
// [참고] http://blog.naver.com/PostView.nhn?blogId=hg1286&logNo=220602654734