package com.example.webserverapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import java.net.Socket

class WebSocketService: Service(){
    companion object{
        const val TAG = "WebSocketService"
        const val SERVER_URL = "http://0.0.0.0:5000"
        const val USER_ID = "user_123"
        const val CHANNEL_ID = "WebSocketServiceChannel"
    }

    lateinit var mSocket: Socket

    override fun onCreate() {
        try{
            mSocket =
        }
    }
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

}