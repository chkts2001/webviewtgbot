package com.example.webserverapp

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.window.OnBackInvokedCallback
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : ToMainCallback, AppCompatActivity() {
    lateinit var idPiece: EditText
    lateinit var ipPiece: EditText
    lateinit var portPiece: EditText
    lateinit var connectBtn: MaterialButton
    lateinit var webField: WebView
    lateinit var connectField: LinearLayout
    lateinit var webLayoutField: LinearLayout

    var webSocketService: WebSocketService? = null
    var isBound = true
    var currentLink = "text"

    companion object{
        const val MODE_WAIT = 2
        const val MODE_SUCCESSFUL = 1
        const val MODE_UNSUCCESSFUL = 0
        const val MODE_CONNECT_FIELD = 3
        const val MODE_WEB_FIELD = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        idPiece = findViewById(R.id.id_edit)
        ipPiece = findViewById(R.id.ip_edit)
        portPiece = findViewById(R.id.port_edit)
        connectBtn = findViewById(R.id.try_connect_btn)

        webField = findViewById(R.id.web_field)
        webField.settings.javaScriptEnabled = true

        connectField = findViewById(R.id.connect_info_field)
        webLayoutField = findViewById(R.id.web_content_field)

        connectField.setIndicateMode(MODE_UNSUCCESSFUL)
        webLayoutField.setIndicateMode(MODE_UNSUCCESSFUL)

        connectBtn.setOnClickListener{
            connectField.setIndicateMode(MODE_WAIT)
            val serviceIntent = Intent(this, WebSocketService::class.java).apply {
                putExtra("IP", ipPiece.text.toString())
                putExtra("PORT", portPiece.text.toString())
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                ContextCompat.startForegroundService(this, serviceIntent)
            }else{
                startService(serviceIntent)
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        webField.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        webField.webChromeClient = object: WebChromeClient(){
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                supportActionBar?.title = currentLink
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebSocketService.LocalBinder
            webSocketService = binder.getService()
            isBound = true
            webSocketService?.setCallback(this@MainActivity) // Передаем колбэк
            Log.d(WebSocketService.TAG, "Service connected, ToMainCallback registered.")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webSocketService = null
            isBound = false
            Log.d(WebSocketService.TAG, "Service disconnected.")
        }
    }

    fun LinearLayout.setIndicateMode(modeIndicate: Int){
        this.setBackgroundDrawable(ContextCompat.getDrawable(context,
            if(modeIndicate == MODE_UNSUCCESSFUL) R.drawable.red_interactive_board_4dp
            else if(modeIndicate == MODE_WAIT) R.drawable.yellow_interactive_board_4dp
            else R.drawable.green_intercative_board_4dp))
    }

    override fun followTheLink(link: String){
        webField.loadUrl(link)
        currentLink = link
    }

    override fun setIndicateMode(modeField: Int, modeIndicate: Int) {
        if(modeField == MODE_CONNECT_FIELD) connectField.setIndicateMode(modeIndicate)
        else webLayoutField.setIndicateMode(modeIndicate)
    }
}