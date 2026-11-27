package com.example.zhidaospeedchecker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PlayerScanAccessibilityService : AccessibilityService() {\n    companion object {\n        @Volatile var instance: PlayerScanAccessibilityService? = null\n            private set\n    }\n\n    override fun onServiceConnected() {\n        super.onServiceConnected()\n        instance = this\n    }\n\n    override fun onDestroy() {\n        instance = null\n        super.onDestroy()\n    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun scanVisiblePage(): String {
        val root = rootInActiveWindow ?: return "没有读取到当前窗口。请让知到播放器保持在屏幕前台，再扫描。"
        val lines = mutableListOf<String>()
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 30 || lines.size >= 120) return
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            if (text.isNotEmpty() || desc.isNotEmpty()) {
                val label = listOf(text, desc).filter { it.isNotEmpty() }.distinct().joinToString(" / ")
                lines.add((if (node.isClickable) "[可点击] " else "") + label)
            }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)
        return if (lines.isEmpty()) "当前页面没有读取到可识别的文字或控件。播放器可能使用了无法读取的自定义界面。"
        else "读取到 ${'$'}{lines.size} 条界面信息：\n" + lines.joinToString("\n")
    }
}