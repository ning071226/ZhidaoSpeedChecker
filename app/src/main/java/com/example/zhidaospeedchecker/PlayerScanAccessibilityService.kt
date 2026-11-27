package com.example.zhidaospeedchecker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PlayerScanAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: PlayerScanAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun scanVisiblePage(): String {
        val visibleWindows = try { windows } catch (_: Exception) { emptyList() }
        val roots = visibleWindows.mapNotNull { window ->
            try { window.root } catch (_: Exception) { null }
        }.distinctBy { it?.windowId }
        val scanRoots = if (roots.isNotEmpty()) roots else listOfNotNull(rootInActiveWindow)
        if (scanRoots.isEmpty()) return "没有读取到可访问的窗口。请保持知到和助手处于分屏状态后重试。"

        val lines = mutableListOf<String>()
        fun walk(node: AccessibilityNodeInfo?, depth: Int, packageName: String) {
            if (node == null || depth > 30 || lines.size >= 160) return
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            if (text.isNotEmpty() || desc.isNotEmpty()) {
                val label = listOf(text, desc).filter { it.isNotEmpty() }.distinct().joinToString(" / ")
                lines.add("[$packageName] " + (if (node.isClickable) "[可点击] " else "") + label)
            }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1, packageName)
        }

        scanRoots.forEach { root ->
            val packageName = root?.packageName?.toString() ?: "未知应用"
            lines.add("窗口：$packageName")
            walk(root, 0, packageName)
        }
        return if (lines.isEmpty()) {
            "已读取可访问窗口，但没有发现可识别的文字或控件。知到播放器可能使用无法读取的自定义界面。"
        } else {
            "读取到 ${lines.size} 条界面信息（已尝试扫描多个窗口）：\n" + lines.joinToString("\n")
        }
    }
}