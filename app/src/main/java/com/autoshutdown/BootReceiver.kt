package com.autoshutdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 부팅 완료 시 서비스 재시작
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs   = context.getSharedPreferences("AutoShutdown", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (enabled) {
            context.startForegroundService(Intent(context, ScreenWatcherService::class.java))
        }
    }
}
