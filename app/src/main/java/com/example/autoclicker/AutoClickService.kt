// دالة شاملة للبحث عن الكلمة في جميع عقد الشاشة بلغة Kotlin
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
