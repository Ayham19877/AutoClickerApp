package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val rootNode = rootInActiveWindow ?: event.source ?: return
        
        val keywordsString = getSavedKeywords() 
        val keywords = keywordsString.split(",").map { it.trim() }

        for (keyword in keywords) {
            if (keyword.isNotEmpty()) {
                val targetNode = findNodeByTextRecursive(rootNode, keyword)
                if (targetNode != null) {
                    triggerAlertAndClick(targetNode)
                    break
                }
            }
        }
    }

    private fun findNodeByTextRecursive(node: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text
        val desc = node.contentDescription
        val targetUpper = targetText.uppercase().trim()

        if ((text != null && text.toString().uppercase().contains(targetUpper)) ||
            (desc != null && desc.toString().uppercase().contains(targetUpper))) {
            if (node.isClickable || (node.parent != null && node.parent.isClickable)) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeByTextRecursive(child, targetText)
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun triggerAlertAndClick(node: AccessibilityNodeInfo) {
        var target = node
        while (!target.isClickable && target.parent != null) {
            target = target.parent
        }
        if (target.isClickable) {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun getSavedKeywords(): String {
        val prefs = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)
        return prefs.getString("keywords", "DETAILS, CLAIM, TASK, YES, VIEW, Accept") ?: ""
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityServiceInfo.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            flags = AccessibilityServiceInfo.FLAG_DEFAULT or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 0
        }
        serviceInfo = info
    }
}
