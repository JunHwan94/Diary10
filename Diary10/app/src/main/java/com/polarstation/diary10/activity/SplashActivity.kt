package com.polarstation.diary10.activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivitySplashBinding
import com.polarstation.diary10.util.NetworkStatus
import kotlinx.coroutines.*

class SplashActivity : BaseActivity() {
    lateinit var binding : ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val intent = Intent(this, LoginActivity::class.java)
        val netStat = NetworkStatus.getConnectivityStatus(this)

        if(netStat == NetworkStatus.TYPE_CONNECTED) {
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            GlobalScope.launch {
                delay(1200)
                startActivity(intent)
            }
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected_quit), Toast.LENGTH_SHORT).show()
    }
}
