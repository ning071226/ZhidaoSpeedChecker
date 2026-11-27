package com.example.zhidaospeedchecker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var controllers: List<MediaController> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
        }
        layout.addView(TextView(this).apply {
            text = "知到倍速助手（播放器扫描测试版）"
            textSize = 23f
        })
        status = TextView(this).apply {
            textSize = 15f
            text = "先开启通知使用权。播放器扫描需要另行开启无障碍权限。"
            setPadding(0, 20, 0, 20)
        }
        layout.addView(status)
        layout.addView(Button(this).apply {
            text = "打开通知使用权设置"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "打开无障碍服务设置"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        layout.addView(Button(this).apply {
            text = "检查媒体会话"
            setOnClickListener { refreshSessions() }
        })
        layout.addView(Button(this).apply {
            text = "扫描当前播放器界面"
            setOnClickListener {
                val scanner = PlayerScanAccessibilityService.instance
                status.text = scanner?.scanVisiblePage()
                    ?: "无障碍扫描服务尚未开启。请先进入无障碍设置，找到“知到倍速助手播放器扫描”并开启，然后返回知到播放器，再切回本工具扫描。"
            }
        })
        layout.addView(Button(this).apply {
            text = "复制全部扫描结果"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("扫描结果", status.text))
                Toast.makeText(this@MainActivity, "已复制全部内容，可以粘贴发送给 ChatGPT。", Toast.LENGTH_LONG).show()
            }
        })
        listOf(2.0f, 2.5f, 3.0f, 4.0f).forEach { speed ->
            layout.addView(Button(this).apply {
                text = "尝试设置为 ${speed}×"
                setOnClickListener { applySpeed(speed) }
            })
        }
        layout.addView(TextView(this).apply {
            text = "说明：扫描仅读取当前屏幕可访问的文字和控件，不会自动点击或修改知到设置。扫描结果不保证能突破知到内置的1.5×限制。"
            textSize = 13f
            setPadding(0, 20, 0, 0)
        })
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(layout, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        setContentView(scrollView)
    }

    private fun refreshSessions() {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            controllers = manager.getActiveSessions(ComponentName(this, SpeedNotificationListener::class.java))
            status.text = if (controllers.isEmpty()) {
                "没有检测到可访问的媒体会话。"
            } else {
                "检测到 ${controllers.size} 个媒体会话：\n" + controllers.joinToString("\n") {
                    "• ${it.packageName}（播放状态：${it.playbackState?.state ?: "未知"}）"
                }
            }
        } catch (e: SecurityException) {
            status.text = "尚未获得通知使用权，请开启后重试。"
        } catch (e: Exception) {
            status.text = "检查失败：${e.message ?: "未知原因"}"
        }
    }

    private fun applySpeed(speed: Float) {
        if (controllers.isEmpty()) refreshSessions()
        if (controllers.isEmpty()) {
            Toast.makeText(this, "没有可控制的媒体会话。", Toast.LENGTH_LONG).show()
            return
        }
        var sent = false
        controllers.forEach {
            try { it.transportControls.setPlaybackSpeed(speed); sent = true } catch (_: Exception) {}
        }
        Toast.makeText(this, if (sent) "已发送 ${speed}× 请求；不代表知到一定会接受。" else "发送失败。", Toast.LENGTH_LONG).show()
    }
}