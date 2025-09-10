package com.example.webserverapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.URISyntaxException

class WebSocketService(): Service(){
    companion object{
        const val TAG = "WebSocketService"
        val USER_ID = "${Build.MANUFACTURER} ${Build.MODEL}"
        const val CHANNEL_ID = "WebSocketServiceChannel"
    }

    lateinit var mSocket: Socket
    var ip: String? = null
    var port: String? = null
    var serverUrl= ""
    var callback: WeakReference<ToMainCallback>? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): WebSocketService = this@WebSocketService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "WebSocketService onBind - returning LocalBinder.")
        return binder
    }

    override fun onCreate() {
        //Handler(Looper.getMainLooper()).postDelayed({createNotifChannel()}, 100)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "")
        Log.d(TAG, "Intent received: $intent")
        createNotifChannel()
        intent?.let {
            ip = it.getStringExtra("IP")
            port = it.getStringExtra("PORT")
        }
        serverUrl = "http://$ip:$port"
        try{
            Log.d(TAG, serverUrl)
            mSocket = IO.socket(serverUrl)
            mSocket.connect()
        }
        catch (e: URISyntaxException) {
            Log.e(TAG, "Error creeating socket", e)
        }
        mSocket.on(Socket.EVENT_CONNECT, object : Emitter.Listener{
            override fun call(vararg args: Any?) {
                Log.d(TAG, "socket_connect")
                try{
                    val data = JSONObject()
                    data.put("user_id", USER_ID);
                    mSocket.emit("register_user", data)
                    val toMain = callback?.get()
                    toMain!!.setIndicateMode(MainActivity.MODE_CONNECT_FIELD, MainActivity.MODE_SUCCESSFUL)
                    sendJsonData("confirm_connect", USER_ID)
                }catch(e: JSONException){
                    Log.e(TAG, "Error sending json data", e);
                }
            }
        }).on("bot_command", object: Emitter.Listener {
            override fun call(vararg args: Any?) {
                val data = args[0] as JSONObject
                Log.d(TAG, "Recieve bot command: ${data.toString()}")
                handleBotCommand(data)
            }
        }).on(Socket.EVENT_DISCONNECT, object: Emitter.Listener{
            override fun call(vararg args: Any?) {
                Log.d(TAG, "Socket disconnected!")
            }
        }).on(Socket.EVENT_CONNECT_ERROR, object: Emitter.Listener{
            override fun call(vararg args: Any?) {
                Log.e(TAG, "Socket connect error: ${args[0].toString()}")
            }
        })
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Клиент активен")
                .setContentText("Соединение с ботом установлено")
                .build()
            startForeground(3, notif)
        }

        return START_STICKY
    }

    fun setCallback(callback: ToMainCallback?) {
        if (callback != null) {
            this.callback = WeakReference(callback)
            Log.d(TAG, "ToMainCallback registered.")
        } else {
            this.callback = null
            Log.d(TAG, "ToMainCallback unregistered.")
        }
    }

    fun sendJsonData(action: String, value: String){
        mSocket.let { socket ->
            Log.d(TAG, "socket: ${socket.connected()}")
            if(socket.connected()){
                try{
                    val data = JSONObject()
                    data.put("action", action)
                    data.put("value", value)
                    socket.emit("app_event", data)
                    Log.d(TAG, "✅ json send to server")
                }catch(e: JSONException){
                    Log.e(TAG, "❌ error create json")
                }
            }else{
                Log.w(TAG, "❌ socket not connected")
            }
        } ?: {
            Log.w(TAG, "❌ socket not init")
        }
    }

    fun handleBotCommand(commandData: JSONObject){
        val packagerManager = this.packageManager
        val applicationInfo = packagerManager.getApplicationInfo(this.packageName, 0)
        val nameApp = packagerManager.getApplicationLabel(applicationInfo).toString()
        try{
            val commandType = commandData.getString("command_type");
            val payload = commandData.getString("payload")

            when (commandType){
                "LAUNCH_ACTIVITY" -> {
                    val launchIntent = Intent(this, MainActivity::class.java)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent.putExtra("message", payload)
                    startActivity(launchIntent)
                } "SHOW_TOAST" -> {
                    Handler(Looper.getMainLooper()).post{Toast.makeText(this, "$nameApp\nToast: $payload", Toast.LENGTH_LONG).show()}
                    Log.d(TAG, "Showing toast: $payload")
                }
            }
        }catch(e: JSONException){
            Log.e(TAG, "Error padsing bot command", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()
        mSocket.off()
        Log.d(TAG, "Service destroy")
    }

    private fun createNotifChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID, "Web Service Channel", NotificationManager.IMPORTANCE_LOW)
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}