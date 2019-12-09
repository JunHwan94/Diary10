package com.polarstation.diary10.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityPhotoViewBinding
import com.polarstation.diary10.fragment.URL_KEY
import com.polarstation.diary10.util.NetworkStatus

class PhotoViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoViewBinding
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_view)

        val url = intent!!.getStringExtra(URL_KEY)
        when {
            url == "" -> Glide.with(this)
                    .load(R.drawable.license)
                    .into(binding.photoViewActivityPhotoView)
            netStat() == NetworkStatus.TYPE_CONNECTED -> Glide.with(this)
                    .load(url)
                    .into(binding.photoViewActivityPhotoView)
            else -> {
                Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
