package com.example.webserverapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : ToMainCallback, AppCompatActivity() {
    lateinit var idPiece: EditText
    lateinit var ipPiece: EditText
    lateinit var portPiece: EditText
    lateinit var connectBtn: MaterialButton
    lateinit var webField: WebView
    lateinit var loadLinkProgress: ProgressBar
    lateinit var loadLinkStatus: ImageView
    lateinit var linkField: TextView
    lateinit var connectField: LinearLayout
    lateinit var webLayoutField: LinearLayout

    var webSocketService: WebSocketService? = null
    var isBound = true
    var currentLink = "text"
    var currentLogin = ""
    var currentPassword = ""

    companion object{
        const val MODE_WAIT = 2
        const val MODE_SUCCESSFUL = 1
        const val MODE_UNSUCCESSFUL = 0
        const val MODE_CONNECT_FIELD = 3
        const val MODE_WEB_FIELD = 4
        const val MODE_AUTH = 5
        const val MODE_NOT_AUTH = 6
        const val MODE_REDACT = 7
        const val MODE_ADD_POEM = 8
        const val MODE_NONE_INFO = 9
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        idPiece = findViewById(R.id.id_edit)
        ipPiece = findViewById(R.id.ip_edit)
        portPiece = findViewById(R.id.port_edit)
        connectBtn = findViewById(R.id.try_connect_btn)
        loadLinkProgress = findViewById(R.id.load_link_progress)
        loadLinkStatus = findViewById(R.id.load_link_status)
        linkField = findViewById(R.id.link_field)

        webField = findViewById(R.id.web_field)
        webField.settings.javaScriptEnabled = true

        WebView.setWebContentsDebuggingEnabled(true)

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
                setLoadLinkStatus(MODE_WAIT)
                Log.d(WebSocketService.TAG, "load started")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(WebSocketService.TAG, "load finished")
                setLoadLinkStatus(MODE_SUCCESSFUL)
                checkPageCode{mode ->
                    when(mode){
                        MODE_AUTH -> fillAuthFormFields()
                    }
                }
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

    override fun followTheLink(link: String, login: String, password: String){
        linkField.text = link
        //setLoadLinkStatus(MODE_WAIT)
        webField.loadUrl(link)
        currentLink = link
        currentLogin = login
        currentPassword = password
    }

    fun setLoadLinkStatus(mode: Int){
        loadLinkProgress.visibility = if(mode == MODE_WAIT) View.VISIBLE else View.GONE
        loadLinkStatus.visibility = if(mode == MODE_WAIT) View.GONE else View.VISIBLE
    }

    fun checkPageCode(callback: (Int) -> Unit){
        var modePage: Int? = null
        val jsCode = """
            (function(){
                try{
                    const findClass = document.getElementsByClassName('h1')[0];
                    if(findClass){
                        const selector = findClass.querySelector('h1');
                        if(selector){
                            const h1Text = selector.textContent;
                            if(h1Text === "Авторизация"){
                                if(document.getElementsByClassName('error')[0]){
                                    return ${MODE_NOT_AUTH.toString()};
                                }else{
                                    return ${MODE_AUTH.toString()};
                                }  
                            } else if(h1Text === "Редактирование"){
                                return ${MODE_REDACT.toString()};
                            } else if(h1Text === "Мои произведения"){
                                return ${MODE_ADD_POEM.toString()};
                            } else{
                                return 'not approved pageMode'
                            }
                        }else{
                            return 'not selector h1 inside class h1'
                        }
                    }else{
                        return 'not class h1' 
                    }
                }catch(e){
                    return 'Error filling from field: ' + e.message;
                }
            })();
        """.trimIndent()
        webField.evaluateJavascript(jsCode) { result ->
            modePage = result.toIntOrNull()
            if(modePage == null) {
                Log.d(WebSocketService.TAG, "❌ $result")
                modePage = MODE_NONE_INFO
            }
            else{
                Handler(Looper.getMainLooper()).post {callback(modePage!!)}
                Log.d(WebSocketService.TAG, "✅ check page func finished: $result")
            }
        }
    }

    fun fillAuthFormFields(){
        val loginToFill = currentLogin ?: return
        val passwordToFill = currentPassword ?: return

        val jsCode = """
            try{
                var loginField = document.getElementsByName('login')[0];
                if(loginField){
                    loginField.value = '$loginToFill';
                }
                var passwordField = document.getElementsByName('password')[0];
                if(passwordField){
                    passwordField.value = '$passwordToFill';
                }
                var submitButton = document.querySelector('input[type="submit"].btn');
                if(submitButton){
                    submitButton.click()
                }
            }catch (e){
                console.error('Error filling form fields: ' + e.message);
            }
        """.trimIndent()
        webField.evaluateJavascript(jsCode, null)
        Log.d(WebSocketService.TAG, "started bot auto_confirm")
    }

    override fun setIndicateMode(modeField: Int, modeIndicate: Int) {
        if(modeField == MODE_CONNECT_FIELD) connectField.setIndicateMode(modeIndicate)
        else webLayoutField.setIndicateMode(modeIndicate)
    }
}