package com.example.zhidaospeedchecker

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
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
        val title = TextView(this).apply {
            text = "知到倍速助手"
            textSize = 25f
        }
        status = TextView(this).apply {
            textSize = 16f
            text = "第一步：先开启本工具的“通知使用权”。"
            setPadding(0, 24, 0, 24)
        }
        val permission = Button(this).apply {
            text = "打开通知使用权设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val refresh = Button(this).apply {
            text = "检查媒体会话"
            setOnClickListener { refreshSessions() }
        }
        layout.addView(title)
        layout.addView(status)
        layout.addView(permission)
        layout.addView(refresh)

        listOf(2.0f, 2.5f, 3.0f, 4.0f).forEach { speed ->
            layout.addView(Button(this).apply {
                text = "尝试设置为 ${speed}×"
                setOnClickListener { applySpeed(speed) }
            })
        }
        val note = TextView(this).apply {
            text = "使用方法：先开启权限，再打开知到并播放课程，返回本工具点击“检查媒体会话”。如果检测到媒体会话，再尝试选择倍速。知到若不开放外部倍速控制，这些按钮可能无法生效。"
            textSize = 14f
            setPadding(0, 24, 0, 0)
        }
        layout.addView(note)
        setContentView(layout)
    }

    private fun refreshSessions() {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            controllers = manager.getActiveSessions(
                ComponentName(this, SpeedNotificationListener::class.java)
            )
            status.text = if (controllers.isEmpty()) {
                "没有检测到可访问的媒体会话。请确认通知使用权已开启，并让知到课程视频正在播放。"
            } else {
                "检测到 ${controllers.size} 个媒体会话：\n" +
                    controllers.joinToString("\n") {
                        "• ${it.packageName}（播放状态：${it.playbackState?.state ?: "未知"}）"
                    }
            }
        } catch (e: SecurityException) {
            status.text = "尚未获得通知使用权。请点击上方按钮开启权限，然后返回本工具。"
        } catch (e: Exception) {
            status.text = "检查失败：${e.message ?: "未知原因"}"
        }
    }

    private fun applySpeed(speed: Float) {
        if (controllers.isEmpty()) refreshSessions()
        if (controllers.isEmpty()) {
            Toast.makeText(this, "没有可控制的媒体会话，请先检查媒体会话。", Toast.LENGTH_LONG).show()
            return
        }
        var success = false
        controllers.forEach { controller ->
            try {
                controller.transportControls.setPlaybackSpeed(speed)
                success = true
            } catch (_: Exception) { }
        }
        Toast.makeText(
            this,
            if (success) "已发送 ${speed}× 控制请求；请回到知到观察是否生效。"
            else "发送失败：知到可能不支持外部倍速控制。",
            Toast.LENGTH_LONG
        ).show()
    }
}
