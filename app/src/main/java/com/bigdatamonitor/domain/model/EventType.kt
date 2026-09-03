package com.bigdatamonitor.domain.model

/** 事件类型枚举 */
enum class EventType(val key: String, val label: String) {
    CLIPBOARD_CHANGE("clipboard_change", "剪贴板变化"),
    CLIPBOARD_ACCESS("clipboard_access", "剪贴板读取"),
    NOTIFICATION("notification", "推送通知"),
    APP_FOREGROUND("app_foreground", "切到前台"),
    APP_BACKGROUND("app_background", "切到后台"),
    NETWORK("network", "网络连接"),
    CORRELATION("correlation", "关联事件");

    companion object {
        fun fromKey(key: String): EventType? = entries.find { it.key == key }
    }
}
