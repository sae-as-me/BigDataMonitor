package com.bigdatamonitor.domain.model

/** 已知广告/追踪域名参考列表 */
object TrackerDomains {
    val domains = setOf(
        // 国际广告网络
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "facebook.com",
        "ads.yahoo.com",

        // 分析追踪
        "google-analytics.com",
        "googletagmanager.com",
        "flurry.com",
        "mixpanel.com",
        "amplitude.com",
        "adjust.com",
        "appsflyer.com",
        "sensors.com",
        "sensorsdata.cn",

        // 国内广告/追踪
        "umeng.com",
        "umeng.co",
        "umengcloud.com",
        "tanx.com",
        "alimama.com",
        "pdd.com"
    )

    /** 判断域名是否为追踪/广告域名 */
    fun isTrackerDomain(host: String): Boolean {
        val lower = host.lowercase()
        return domains.any { lower.contains(it) || it.contains(lower) }
    }
}
