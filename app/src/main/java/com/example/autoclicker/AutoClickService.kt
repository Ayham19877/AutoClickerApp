package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Switch
import android.widget.Toast

class AutoClickService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingSwitch: Switch? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isClickingActive = false

    private val clickRunnable = object : Runnable {
        override fun run() {
            if (isClickingActive) {
                // 1. النقرة الأولى على الإحداثيات (549, 889)
                clickOnCoordinates(549f, 889f)

                // 2. النقرة الثانية بعد 220 ميلي ثانية على الإحداثيات (593, 1453)
                handler.postDelayed({
                    if (isClickingActive) {
                        clickOnCoordinates(593f, 1453f)
                    }
                }, 220)

                // تكرار الحلقة باستمرار
                handler.postDelayed(this, 600)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityServiceInfo.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            flags = AccessibilityServiceInfo.FLAG_DEFAULT or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            capabilities = AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES
            notificationTimeout = 0
        }
        serviceInfo = info

        // التحقق من صلاحية النافذة العائمة، وإذا لم تكن مجهزة يتم فتح إعداداتها فوراً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Toast.makeText(this, "يرجى السماح بالتطبيق بالظهور فوق التطبيقات", Toast.LENGTH_LONG).show()
        } else {
            showFloatingWindow()
        }
    }

    private fun showFloatingWindow() {
        try {
            if (floatingSwitch != null) return
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            floatingSwitch = Switch(this).apply {
                text = " التكبيس المستمر "
                isChecked = false
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                setPadding(35, 35, 35, 35)
                setOnCheckedChangeListener { _, isChecked ->
                    isClickingActive = isChecked
                    if (isChecked) {
                        Toast.makeText(this@AutoClickService, "تم بدء التكبيس المستمر", Toast.LENGTH_SHORT).show()
                        handler.post(clickRunnable)
                    } else {
                        Toast.makeText(this@AutoClickService, "تم إيقاف التكبيس", Toast.LENGTH_SHORT).show()
                        handler.removeCallbacks(clickRunnable)
                    }
                }
            }

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 300
            }

            windowManager?.addView(floatingSwitch, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun clickOnCoordinates(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val desc = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(desc, null, null)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        removeFloatingWindow()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWindow()
    }

    private fun removeFloatingWindow() {
        try {
            if (floatingSwitch != null) {
                windowManager?.removeView(floatingSwitch)
                floatingSwitch = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
