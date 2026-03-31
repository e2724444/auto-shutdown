package com.autoshutdown

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 접근성 서비스
 * - ACTION_SHUTDOWN 수신 시 전원 메뉴 열고 "전원 끄기" 자동 클릭
 */
class ShutdownAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_SHUTDOWN = "com.autoshutdown.ACTION_SHUTDOWN"

        // 삼성 One UI 전원 끄기 텍스트 (한/영)
        val POWER_OFF_TEXTS = listOf(
            "전원 끄기", "전원끄기", "Power off", "Turn off", "Shut down"
        )
    }

    private var waitingForMenu = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHUTDOWN) {
            waitingForMenu = true
            // 전원 다이얼로그 열기
            performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            // 메뉴 로딩 대기 후 클릭
            Handler(Looper.getMainLooper()).postDelayed({ tryClickPowerOff() }, 1500)
        }
        return START_NOT_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!waitingForMenu) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.contains("systemui", ignoreCase = true) ||
            event.className?.toString()?.contains("GlobalActions") == true) {
            waitingForMenu = false
            Handler(Looper.getMainLooper()).postDelayed({ tryClickPowerOff() }, 400)
        }
    }

    private fun tryClickPowerOff(): Boolean {
        val root = rootInActiveWindow ?: return false
        return findAndClick(root)
    }

    private fun findAndClick(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        for (kw in POWER_OFF_TEXTS) {
            if (text.contains(kw, ignoreCase = true) || desc.contains(kw, ignoreCase = true)) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClick(child)) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
