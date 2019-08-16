package com.polarstation.diary10;

import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Handler;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import com.polarstation.diary10.databinding.ActivitySplashBinding;

import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;
import static com.polarstation.diary10.util.NetworkStatus.getConnectivityStatus;

public class SplashActivity extends BaseActivity {
    private ActivitySplashBinding binding;
    private Handler handler = new Handler();
    private int netStat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        netStat = getConnectivityStatus(this);
        if(netStat == TYPE_CONNECTED) {
            handler.postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                finish();
            }, 1200);
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected_quit), Toast.LENGTH_SHORT).show();
    }
}
