package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AutoClickService : AccessibilityService() {

    // اكتب الكلمة المطلوبة للنقر عليها فور ظهورها
    private val TARGET_TEXT = "Accept"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rootNode = rootInActiveWindow ?: return

        val nodes = rootNode.findAccessibilityNodeInfosByText(TARGET_TEXT)
        if (nodes != null && nodes.isNotEmpty()) {
            for (node in nodes) {
                if (node.text != null && node.text.toString().contains(TARGET_TEXT, ignoreCase = true)) {
                    if (!clickNodeOrParent(node)) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    break
                }
            }
        }
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    override fun onInterrupt() {}
}

