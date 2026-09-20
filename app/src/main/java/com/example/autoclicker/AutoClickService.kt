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

    // الكلمات المستهدفة التي طلبتها للبحث الفوري
    private val targetWords = listOf("Accept", "DETAILS", "CLAIM", "IT", "VIEW")

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            // استقبال جميع أحداث الشاشة وتغييرات النافذة بشكل لحظي تماماً مثل الماكرو
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
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
                text = " الماكرو الذكي (شغال) "
                isChecked = false
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
                setPadding(35, 35, 35, 35)
                setOnCheckedChangeListener { _, isChecked ->
                    isClickingActive = isChecked
                    if (isChecked) {
                        Toast.makeText(this@AutoClickService, "تم تفعيل الماكرو الفوري", Toast.LENGTH_SHORT).show()
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

    // هذه الدالة تعمل بشكل لحظي فور حدوث أي تغيير على الشاشة (مثل الماكرو)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isClickingActive) return

        val rootNode = rootInActiveWindow ?: return
        try {
            if (searchAndClickNode(rootNode)) {
                // إذا تم العثور على الكلمة والضغط عليها، نتوقف مؤقتاً لتفادي التكرار السريع جداً
                isClickingActive = false
                floatingSwitch?.isChecked = false
                handlerPostReset()
            }
        } finally {
            rootNode.recycle()
        }
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private fun handlerPostReset() {
        // إعادة تفعيل الماكرو تلقائياً بعد ثانيتين ليتابع التقاط الطلبات الجديدة
        handler.postDelayed({
            isClickingActive = true
            floatingSwitch?.isChecked = true
        }, 2000)
    }

    private fun searchAndClickNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        for (target in targetWords) {
            if (text.contains(target, ignoreCase = true) || contentDesc.contains(target, ignoreCase = true)) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty) {
                    val centerX = rect.exactCenterX()
                    val centerY = rect.exactCenterY()
                    
                    // تنفيذ النقرة الفورية على إحداثيات الكلمة
                    clickOnCoordinates(centerX, centerY)
                    return true
                }
            }
        }

        // البحث العميق داخل العقد الفرعية للشاشة
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
