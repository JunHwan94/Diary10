package com.polarstation.diary10;

import android.graphics.Typeface;
import androidx.appcompat.app.AppCompatActivity;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;
import gun0912.tedkeyboardobserver.TedRxKeyboardObserver;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class BaseActivity extends AppCompatActivity{
    private static Typeface typeface = null;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        if(typeface == null){
            typeface = Typeface.createFromAsset(this.getAssets(), "fonts/scdream3.otf");
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
    /*  ~36 line 출처
    http://blog.naver.com/PostView.nhn?blogId=hg1286&logNo=220602654734  */
}