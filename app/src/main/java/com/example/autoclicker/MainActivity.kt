package com.example.autoclicker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var switchService: Switch
    private lateinit var switchAutoClick: Switch
    private lateinit var switchVibrate: Switch
    private lateinit var switchSound: Switch
    private lateinit var etWords: EditText
    private lateinit var etMinAmount: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)

        switchService = findViewById(R.id.switchService)
        switchAutoClick = findViewById(R.id.switchAutoClick)
        switchVibrate = findViewById(R.id.switchVibrate)
        switchSound = findViewById(R.id.switchSound)
        etWords = findViewById(R.id.etWords)
        etMinAmount = findViewById(R.id.etMinAmount)
        btnSave = findViewById(R.id.btnSave)

        // تحميل القيم المحفوظة مسبقاً
        val savedWords = sharedPreferences.getString("target_words", "Accept, DETAILS, CLAIM, IT, VIEW")
        etWords.setText(savedWords)
        
        val savedAmount = sharedPreferences.getFloat("min_amount", 0f)
        if (savedAmount > 0f) {
            etMinAmount.setText(savedAmount.toString())
        }

        switchAutoClick.isChecked = sharedPreferences.getBoolean("autoclick_enabled", true)
        switchVibrate.isChecked = sharedPreferences.getBoolean("vibrate_enabled", true)
        switchSound.isChecked = sharedPreferences.getBoolean("sound_enabled", true)

        // حفظ الإعدادات عند الضغط على الزر
        btnSave.setOnClickListener {
            val words = etWords.text.toString()
            val minAmountStr = etMinAmount.text.toString()
            val minAmount = if (minAmountStr.isNotEmpty()) minAmountStr.toFloat() else 0f

            sharedPreferences.edit().apply {
                putString("target_words", words)
                putFloat("min_amount", minAmount)
                putBoolean("autoclick_enabled", switchAutoClick.isChecked)
                putBoolean("vibrate_enabled", switchVibrate.isChecked)
                putBoolean("sound_enabled", switchSound.isChecked)
                apply()
            }
            Toast.makeText(this, "تم حفظ الإعدادات والحد الأدنى بنجاح", Toast.LENGTH_SHORT).show()
        }

        switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        switchService.isChecked = isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val prefString = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return prefString?.contains("$packageName/.AutoClickService") == true
    }
}
