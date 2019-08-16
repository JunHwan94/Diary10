package com.polarstation.diary10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.polarstation.diary10.databinding.ActivityPhotoViewBinding;
import com.polarstation.diary10.util.NetworkStatus;

import static com.polarstation.diary10.fragment.AccountFragment.URL_KEY;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class PhotoViewActivity extends AppCompatActivity {
    private ActivityPhotoViewBinding binding;
    private int netStat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_view);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getStringExtra(URL_KEY);

            netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
            if(netStat == TYPE_CONNECTED) {
                Glide.with(this)
                        .load(url)
                        .into(binding.photoViewActivityPhotoView);
            }else {
                Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
