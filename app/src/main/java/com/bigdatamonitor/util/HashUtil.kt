package com.bigdatamonitor.util

import java.security.MessageDigest

/** 哈希工具类 */
object HashUtil {
    /** 计算字符串的 SHA-256 哈希，返回十六进制字符串 */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /** 截取内容预览（前 N 字符） */
    fun preview(content: String, maxLen: Int = 20): String {
        return if (content.length <= maxLen) content else content.take(maxLen) + "..."
    }
}
