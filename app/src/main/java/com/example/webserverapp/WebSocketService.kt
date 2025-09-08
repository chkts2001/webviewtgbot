package com.example.webserverapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

class WebSocketService: Service(){
    companion object{
        const val TAG = "WebSocketService"
        const val SERVER_URL = "http://192.168.40.151:5000"
        const val USER_ID = "user_123"
        const val CHANNEL_ID = "WebSocketServiceChannel"
    }

    lateinit var mSocket: Socket

    override fun onCreate() {
        //Handler(Looper.getMainLooper()).postDelayed({createNotifChannel()}, 100)
        createNotifChannel()
        try{
            mSocket = IO.socket(SERVER_URL)
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Клиент активен")
                .setContentText("Соединение с ботом установлено")
                .build()
            startForeground(3, notif)
        }
        mSocket.connect()

        return START_STICKY
    }

    fun handleBotCommand(commandData: JSONObject){
        try{
            val commandType = commandData.getString("type");
            val payload = commandData.getString("payload")

            when (commandType){
                "LAUNCH_ACTIVITY" -> {
                    val launchIntent = Intent(this, MainActivity::class.java)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent.putExtra("message", payload)
                    startActivity(launchIntent)
                } "SHOW_TOAST" -> {
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
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotifChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID, "Web Service Channel", NotificationManager.IMPORTANCE_LOW)
            val manager: NotificationManager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

}