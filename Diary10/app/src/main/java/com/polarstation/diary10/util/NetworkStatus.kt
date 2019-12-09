package com.polarstation.diary10.util

import android.content.Context
import android.net.ConnectivityManager

class NetworkStatus{
     companion object{
         const val TYPE_CONNECTED = 1
         const val TYPE_NOT_CONNECTED = 0
         val getConnectivityStatus: (Context) -> Int = {
             val manager = it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
             val networkInfo = manager.activeNetworkInfo
             if (networkInfo != null) TYPE_CONNECTED
             else TYPE_NOT_CONNECTED
         }
    }
}