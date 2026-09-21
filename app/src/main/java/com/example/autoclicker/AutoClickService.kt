package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.RingtoneManager
import android.view.accessibility.accessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.GestureDescription

class AutoClickService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPreferences = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        
        val autoClickEnabled = sharedPreferences.getBoolean("autoclick_enabled", true)
        val wordsString = sharedPreferences.getString("target_words", "Accept, DETAILS, CLAIM, IT, VIEW") ?: ""
        val targetWords = wordsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (autoClickEnabled) {
            searchAndClickAndAlert(rootNode, targetWords)
        }
        rootNode.recycle()
    }

    private fun searchAndClickAndAlert(node: AccessibilityNodeInfo, targetWords: List<String>): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val fullText = "$text $contentDesc"
        
        val minAmount = sharedPreferences.getFloat("min_amount", 0f)

        var wordMatched = false
        if (targetWords.isEmpty()) {
            wordMatched = true
        } else {
            for (target in targetWords) {
                if (fullText.contains(target, ignoreCase = true)) {
                    wordMatched = true
                    break
                }
            }
        }

        if (wordMatched) {
            // التحقق من الحد الأدنى للمبلغ إذا تم تحديده
            if (minAmount > 0f) {
                val extractedPrices = extractNumbers(fullText)
                val priceValid = extractedPrices.any { it >= minAmount }
                if (!priceValid) {
                    return searchChildren(node, targetWords)
                }
            }

            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                clickOnCoordinates(rect.exactCenterX(), rect.exactCenterY())
                triggerAlert()
                return true
            }
        }

        return searchChildren(node, targetWords)
    }

    private fun searchChildren(node: AccessibilityNodeInfo, targetWords: List<String>): Boolean {
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                try {
                    if (searchAndClickAndAlert(child, targetWords)) {
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

    private fun extractNumbers(text: String): List<Float> {
        val numbers = mutableListOf<Float>()
        val regex = Regex("""\d+([,.]\d+)?""")
        val matches = regex.findAll(text)
        for (match in matches) {
            val cleanNum = match.value.replace(",", ".")
            val num = cleanNum.toFloatOrNull()
            if (num != null) {
                numbers.add(num)
            }
        }
        return numbers
    }

    private fun clickOnCoordinates(x: Float, y: Float) {
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val builder = GestureDescription.Builder().addStroke(stroke)
        dispatchGesture(builder.build(), null, null)
    }

    private fun triggerAlert() {
        val vibrateEnabled = sharedPreferences.getBoolean("vibrate_enabled", true)
        val soundEnabled = sharedPreferences.getBoolean("sound_enabled", true)

        if (vibrateEnabled) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        }

        if (soundEnabled) {
            try {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(applicationContext, notification)
                r.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {}
}
