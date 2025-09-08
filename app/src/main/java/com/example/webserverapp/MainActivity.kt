package com.example.webserverapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val serviceIntent = Intent(this, WebSocketService::class.java)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startForegroundService(serviceIntent)
        }else{
            startService(serviceIntent)
        }
    }
}