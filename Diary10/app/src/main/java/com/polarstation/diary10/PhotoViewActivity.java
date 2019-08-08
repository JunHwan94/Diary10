package com.polarstation.diary10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.polarstation.diary10.databinding.ActivityPhotoViewBinding;

import static com.polarstation.diary10.fragment.AccountFragment.URL_KEY;

public class PhotoViewActivity extends AppCompatActivity {
    private ActivityPhotoViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_view);

        Intent intent = getIntent();
        if (intent != null) {
            String url = intent.getStringExtra(URL_KEY);

            Glide.with(this)
                    .load(url)
                    .into(binding.photoViewActivityPhotoView);
        }
    }
}
