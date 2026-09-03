package com.bigdatamonitor.domain.model

/** 敏感权限分类定义 */
enum class SensitivePermission(val permission: String, val category: String, val sensitivityLevel: Int) {
    // 高危权限
    FINE_LOCATION("android.permission.ACCESS_FINE_LOCATION", "location", 3),
    COARSE_LOCATION("android.permission.ACCESS_COARSE_LOCATION", "location", 3),
    BACKGROUND_LOCATION("android.permission.ACCESS_BACKGROUND_LOCATION", "location", 3),
    RECORD_AUDIO("android.permission.RECORD_AUDIO", "microphone", 3),
    CAMERA("android.permission.CAMERA", "camera", 3),
    READ_CONTACTS("android.permission.READ_CONTACTS", "contacts", 3),
    WRITE_CONTACTS("android.permission.WRITE_CONTACTS", "contacts", 3),
    READ_CALL_LOG("android.permission.READ_CALL_LOG", "call_log", 3),
    READ_PHONE_STATE("android.permission.READ_PHONE_STATE", "phone", 3),
    CALL_PHONE("android.permission.CALL_PHONE", "phone", 3),
    READ_SMS("android.permission.READ_SMS", "sms", 3),
    SEND_SMS("android.permission.SEND_SMS", "sms", 3),
    RECEIVE_SMS("android.permission.RECEIVE_SMS", "sms", 3),

    // 中危权限
    READ_EXTERNAL_STORAGE("android.permission.READ_EXTERNAL_STORAGE", "storage", 2),
    WRITE_EXTERNAL_STORAGE("android.permission.WRITE_EXTERNAL_STORAGE", "storage", 2),
    MANAGE_EXTERNAL_STORAGE("android.permission.MANAGE_EXTERNAL_STORAGE", "storage", 2),
    READ_CALENDAR("android.permission.READ_CALENDAR", "calendar", 2),
    WRITE_CALENDAR("android.permission.WRITE_CALENDAR", "calendar", 2),
    BODY_SENSORS("android.permission.BODY_SENSORS", "sensors", 2),
    ACTIVITY_RECOGNITION("android.permission.ACTIVITY_RECOGNITION", "sensors", 2),

    // 低危权限
    INTERNET("android.permission.INTERNET", "network", 1),
    ACCESS_NETWORK_STATE("android.permission.ACCESS_NETWORK_STATE", "network", 1),
    POST_NOTIFICATIONS("android.permission.POST_NOTIFICATIONS", "notifications", 1);

    companion object {
        /** 权限名 → 定义映射 */
        private val map = entries.associateBy { it.permission }

        fun fromPermission(perm: String): SensitivePermission? = map[perm]

        /** 高敏感度权限清单 (sensitivityLevel == 3) */
        val highSensitive = entries.filter { it.sensitivityLevel == 3 }

        /** 所有已知的敏感权限名集合 */
        val allPermissionNames = entries.map { it.permission }.toSet()
    }
}

/** 权限信息数据类 */
data class PermissionInfo(
    val name: String,
    val isGranted: Boolean,
    val sensitivityLevel: Int,
    val category: String
)
