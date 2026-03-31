package com.autoshutdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 알람 시간 도달 → 접근성 서비스에 전원 끄기 요청
 */
class ShutdownAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, ShutdownAccessibilityService::class.java).apply {
            action = ShutdownAccessibilityService.ACTION_SHUTDOWN
        }
        context.startService(serviceIntent)
    }
}
