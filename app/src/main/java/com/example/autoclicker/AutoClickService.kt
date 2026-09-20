package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Switch
import android.widget.Toast

class AutoClickService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingSwitch: Switch? = null
    private var isClickingActive = false
    private val handler = Handler(Looper.getMainLooper())

    // الكلمات المستهدفة بدقة
    private val targetWords = listOf("Accept", "DETAILS", "CLAIM", "IT", "VIEW")

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } else {
            showFloatingWindow()
        }
    }

    private fun showFloatingWindow() {
        try {
            if (floatingSwitch != null) return
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            floatingSwitch = Switch(this).apply {
                text = " الماكرو الذكي (يعمل) "
                isChecked = false
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                setPadding(35, 35, 35, 35)
                setOnCheckedChangeListener { _, isChecked ->
                    isClickingActive = isChecked
                    if (isChecked) {
                        Toast.makeText(this@AutoClickService, "تم تفعيل البحث والضغط", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AutoClickService, "تم إيقاف الماكرو", Toast.LENGTH_SHORT).show()
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isClickingActive) return

        val rootNode = rootInActiveWindow ?: return
        try {
            if (searchAndClickNode(rootNode)) {
                // إيقاف مؤقت لمنع التكرار المفرط ثم إعادة التفعيل تلقائياً
                isClickingActive = false
                handler.postDelayed({
                    if (floatingSwitch?.isChecked == true) {
                        isClickingActive = true
                    }
                }, 1500)
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun searchAndClickNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        for (target in targetWords) {
            // مطابقة النصوص بغض النظر عن حالة الأحرف الكبيرة والصغيرة
            if (text.contains(target, ignoreCase = true) || contentDesc.contains(target, ignoreCase = true)) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    val centerX = rect.exactCenterX()
                    val centerY = rect.exactCenterY()
                    
                    clickOnCoordinates(centerX, centerY)
                    return true
                }
            }
        }

        // البحث داخل العقد والأبناء بشكل كامل
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    if (searchAndClickNode(child)) {
                        child.recycle()
                        return true
                    }
                } finally {
                    child.recycle()
                }
            }
        }
        return false
    }

    private fun clickOnCoordinates(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply {
                moveTo(x, y)
                lineTo(x, y)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 100)
            val desc = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(desc, null, null)
        }
    }

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

