package com.polarstation.diary10.activity;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity{
    private static Typeface typeface = null;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        if(typeface == null){
            typeface = Typeface.createFromAsset(this.getAssets(), "fonts/JalnanOTF.otf");
        }

        setGlobalFont(getWindow().getDecorView());
    }

    public static void setGlobalFont(View view){
        if(view != null){
            if(view instanceof ViewGroup){
                ViewGroup vg = (ViewGroup)view;
                int vgCnt = vg.getChildCount();
                for(int i = 0; i < vgCnt; i++){
                    View v = vg.getChildAt(i);
                    if(v instanceof TextView){
                        ((TextView) v).setTypeface(typeface);
                    }
                    setGlobalFont(v);
                }
            }
        }
    }
    // [참고] http://blog.naver.com/PostView.nhn?blogId=hg1286&logNo=220602654734
}