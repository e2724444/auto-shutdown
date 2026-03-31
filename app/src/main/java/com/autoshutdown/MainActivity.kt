package com.autoshutdown

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // 선택 가능한 딜레이 목록 (분 단위)
    val delayOptions = listOf(
        "10분"   to 10,
        "20분"   to 20,
        "30분"   to 30,
        "1시간"  to 60,
        "2시간"  to 120,
        "3시간"  to 180,
        "4시간"  to 240
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("AutoShutdown", Context.MODE_PRIVATE)

        val switchEnabled    = findViewById<SwitchMaterial>(R.id.switchEnabled)
        val spinnerDelay     = findViewById<Spinner>(R.id.spinnerDelay)
        val btnSave          = findViewById<MaterialButton>(R.id.btnSave)
        val tvStatus         = findViewById<TextView>(R.id.tvStatus)
        val tvAccessibility  = findViewById<TextView>(R.id.tvAccessibility)
        val btnAccessibility = findViewById<MaterialButton>(R.id.btnAccessibility)

        // 스피너 설정
        val labels = delayOptions.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDelay.adapter = adapter

        // 저장된 값 복원
        val savedMinutes = prefs.getInt("delayMinutes", 30)
        val savedEnabled = prefs.getBoolean("enabled", false)
        val savedIndex   = delayOptions.indexOfFirst { it.second == savedMinutes }.coerceAtLeast(0)
        spinnerDelay.setSelection(savedIndex)
        switchEnabled.isChecked = savedEnabled

        // 접근성 버튼
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        // 저장 버튼
        btnSave.setOnClickListener {
            val selectedMinutes = delayOptions[spinnerDelay.selectedItemPosition].second
            val enabled = switchEnabled.isChecked

            if (enabled && !isAccessibilityEnabled()) {
                Toast.makeText(this, "먼저 접근성 권한을 허용해주세요!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putInt("delayMinutes", selectedMinutes)
                .putBoolean("enabled", enabled)
                .apply()

            // 서비스 시작/중지
            val serviceIntent = Intent(this, ScreenWatcherService::class.java)
            if (enabled) {
                startForegroundService(serviceIntent)
                Toast.makeText(this, "✅ 화면 꺼지고 ${delayOptions[spinnerDelay.selectedItemPosition].first} 후 전원 OFF", Toast.LENGTH_SHORT).show()
            } else {
                stopService(serviceIntent)
                Toast.makeText(this, "⚫ 자동 전원 끄기 꺼짐", Toast.LENGTH_SHORT).show()
            }
            updateStatus(tvStatus, tvAccessibility)
        }

        updateStatus(tvStatus, tvAccessibility)
    }

    override fun onResume() {
        super.onResume()
        val tvStatus        = findViewById<TextView>(R.id.tvStatus)
        val tvAccessibility = findViewById<TextView>(R.id.tvAccessibility)
        updateStatus(tvStatus, tvAccessibility)
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${ShutdownAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabled?.contains(service) == true
    }

    private fun updateStatus(tvStatus: TextView, tvAccessibility: TextView) {
        val enabled = prefs.getBoolean("enabled", false)
        val minutes = prefs.getInt("delayMinutes", 30)
        val label   = delayOptions.firstOrNull { it.second == minutes }?.first ?: "${minutes}분"

        tvAccessibility.text = if (isAccessibilityEnabled())
            "✅ 접근성 권한: 허용됨"
        else
            "❌ 접근성 권한: 허용 필요"

        tvStatus.text = if (enabled)
            "🟢 활성화됨\n화면 꺼지고 $label 후 전원 OFF"
        else
            "⚫ 비활성화"
    }
}
