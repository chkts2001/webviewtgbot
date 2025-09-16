package com.example.webserverapp

import android.widget.LinearLayout

interface ToMainCallback {
    fun setIndicateMode(modeField: Int, modeIndicate: Int)
    fun followTheLink(link: String, login: String, password: String)
    fun sendJsonToAiogram(mode: Int, info: String)
}