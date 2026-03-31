package com.autoshutdown

import android.app.*
import android.content.*
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * 포그라운드 서비스
 * - ACTION_SCREEN_OFF 동적 등록 → 화면 꺼지면 알람 세팅
 * - ACTION_SCREEN_ON  동적 등록 → 화면 다시 켜지면 알람 취소
 */
class ScreenWatcherService : Service() {

    companion object {
        const val CHANNEL_ID    = "AutoShutdownChannel"
        const val NOTIF_ID      = 1
        const val ALARM_REQ     = 1001
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON  -> onScreenOn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("대기 중 — 화면이 꺼지면 타이머 시작"))

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        cancelAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 화면 꺼짐: 알람 세팅 ──────────────────────────────────────────
    private fun onScreenOff() {
        val prefs   = getSharedPreferences("AutoShutdown", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        if (!enabled) return

        val minutes     = prefs.getInt("delayMinutes", 30)
        val triggerAt   = System.currentTimeMillis() + minutes * 60 * 1000L

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, getPendingIntent())

        val label = formatMinutes(minutes)
        updateNotification("⏱ 타이머 시작 — $label 후 전원 OFF")
    }

    // ── 화면 켜짐: 알람 취소 ─────────────────────────────────────────
    private fun onScreenOn() {
        cancelAlarm()
        updateNotification("대기 중 — 화면이 꺼지면 타이머 시작")
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent())
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, ShutdownAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, ALARM_REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ── 알림 ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "자동 전원 끄기",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "화면 꺼짐 감지 서비스" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(msg: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("자동 전원 끄기")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(msg: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(msg))
    }

    private fun formatMinutes(minutes: Int): String = when {
        minutes < 60 -> "${minutes}분"
        minutes % 60 == 0 -> "${minutes / 60}시간"
        else -> "${minutes / 60}시간 ${minutes % 60}분"
    }
}
