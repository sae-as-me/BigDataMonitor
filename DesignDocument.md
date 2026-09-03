
# BigDataMonitor 设计方案

## 一、项目概述

### 1.1 项目名称
BigDataMonitor — 个人隐私数据监控器

### 1.2 一句话定位
一款 Android 端隐私行为感知工具，帮助用户理解"我的哪些个人隐私信息、在什么时间、被哪些 App 获取或用于了个性化推荐"。

### 1.3 核心设计理念

本应用源于一个真实的用户困扰：用户线下的对话内容（如"爬山"）很快出现在电商和短视频 App 的推荐中。这意味着用户的数据正在被采集、共享或推理，而用户对此毫无感知。

BigDataMonitor 不会假装能做到"精确读取每个 App 的内部行为"——Android 沙箱机制决定了这不可能。相反，它采用**信号采集 + 关联分析 + 可视化呈现**的策略：

- **采集**：利用 Android 合法 API（无障碍服务、通知监听、剪贴板监听、使用统计、权限审计）收集多条隐私相关的行为信号
- **关联**：用户标注敏感事件（如"14:00 与朋友谈论爬山"），系统在时间线上高亮该事件前后各 App 的异常行为
- **呈现**：以时间线、应用详情、风险评分等方式，让隐形的隐私数据流变为可见

### 1.4 产品属性
- **公益属性**：无广告、无内购、无数据上传
- **最小权限**：仅申请实现功能所必需的系统权限，不申请互联网权限
- **开源透明**：所有监控逻辑对用户可见，监控数据仅存于本地

---

## 二、核心问题与技术边界

### 2.1 Android 沙箱隔离的现实

Android 的安全模型基于沙箱隔离：每个 App 运行在独立进程中，拥有独立的 UID，无法直接访问其他 App 的内存、文件或内部行为。这意味着：

| 用户期望 | 技术现实 | 本应用的应对 |
|---------|---------|------------|
| "知道哪个 App 读取了我的对话内容" | 无法直接监听其他 App 的麦克风或语音数据 | 通过 UsageStatsManager 追踪 App 使用时序，通过无障碍服务检测屏幕内容变化，做间接关联 |
| "知道哪个 App 读取了我的剪贴板" | Android 12+ 系统会弹出剪贴板访问提示，但 API 不直接告知是哪个 App 读取的 | 无障碍服务捕获该系统 Toast 文本，提取 App 包名 | 
| "知道哪个 App 把我的数据共享给了其他 App" | 无法截获 App 间数据共享 | 通过权限审计+网络流向+使用时序做关联推断 |
| "知道哪个 App 在后台用了麦克风/摄像头" | AppOpsManager 可查询但仅限自身 App 的操作记录（Android 10-），部分 hidden API 需反射 | 在可获取的范围内尽力查询，不可获取时如实告知 |

### 2.2 能做到 vs 做不到的诚实清单

**能做到：**
1. 监听剪贴板内容变化并记录时间戳和内容摘要
2. 通过无障碍服务捕获 Android 12+ 的"App 已从剪贴板粘贴"系统提示，识别读取剪贴板的 App
3. 通过 NotificationListenerService 捕获所有通知内容（含电商推荐、短视频推送等），记录来源 App 和时间
4. 通过 UsageStatsManager 追踪各 App 前后台切换时间线
5. 扫描所有已安装 App 的权限声明清单，标识持有敏感权限的 App
6. 通过 AppOpsManager 查询（在系统允许的范围内）敏感操作的访问记录
7. 用户手动标注"敏感事件"（如线下对话主题），系统在时间线上关联展示该事件前后各 App 的行为变化
8. （可选）通过 VpnService 建立本地 VPN，捕获各 App 的 DNS 请求和连接目标域名

**做不到：**
1. 直接读取其他 App 采集的麦克风音频数据
2. 截获 App 之间的数据共享（如 SDK 跨 App 追踪）
3. 精确判定"App 推荐的内容来源于用户某次线下对话"（这需要 App 内部数据，无法从外部获取）
4. 在 Android 10 设备上获取 AppOps 的完整的跨 App 操作记录

### 2.3 法规与伦理边界

- 应用仅监控"已授予相应权限的合法信号"，不使用任何绕过 Android 安全机制的 hack
- 不上传任何用户数据，不联网通信（不申请 `INTERNET` 权限）
- 通知内容、剪贴板内容等敏感数据在本地存储时可选择"仅存摘要/哈希"而非全文
- 应用在首次启动时明确告知用户各项监控能力的边界与局限

---

## 三、技术架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                    UI 层 (Jetpack Compose)            │
│  Dashboard │ Timeline │ AppDetail │ Correlation │ Settings │
└──────────────────────┬──────────────────────────────┘
                       │ ViewModel + StateFlow
┌──────────────────────┴──────────────────────────────┐
│                  业务逻辑层 (Domain)                    │
│  CorrelationEngine │ RiskScorer │ PrivacyAnalyzer     │
└──────────────────────┬──────────────────────────────┘
                       │ Repository
┌──────────────────────┴──────────────────────────────┐
│                  数据层 (Room Database)                │
│  AppDao │ EventDao │ CorrelationDao │ TopicDao       │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│              系统服务层 (Android Services)              │
│                                                       │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ Accessibility │  │ Notification │  │  Clipboard   │ │
│  │   Service    │  │   Listener   │  │   Monitor    │ │
│  └─────────────┘  └──────────────┘  └──────────────┘ │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ UsageStats  │  │   Permission │  │   Network    │ │
│  │  Tracker    │  │    Auditor   │  │   Monitor    │ │
│  │ (WorkManager) │  │ (WorkManager) │  │  (VpnService)│ │
│  └─────────────┘  └──────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────┘
```

### 3.2 技术栈

| 层 | 技术选型 | 版本要求 |
|---|---------|---------|
| 语言 | Kotlin | 1.9+ |
| UI | Jetpack Compose + Material 3 (Material You) | Compose BOM 2024.x |
| 主题 | dynamicColor (Android 12+) / 静态主题色 (Android 10-11) | Material 3 |
| 数据库 | Room | 2.6+ |
| 依赖注入 | Hilt | 2.50+ |
| 后台任务 | WorkManager | 2.9+ |
| 导航 | Compose Navigation | 2.7+ |
| 构建 | Gradle (Kotlin DSL) | 8.x |
| 最低 SDK | API 29 (Android 10) | |
| 目标 SDK | API 34 (Android 14) | |
| Java 版本 | 17 | |

### 3.3 权限清单（最小权限原则）

本应用自身仅申请以下权限，**不申请 INTERNET 权限**：

| 权限 | 用途 | 类型 |
|------|------|------|
| `BIND_ACCESSIBILITY_SERVICE` | 无障碍服务，捕获剪贴板提示、屏幕内容变化 | 特殊权限，用户手动授权 |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | 通知监听服务，捕获推送通知内容 | 特殊权限，用户手动授权 |
| `PACKAGE_USAGE_STATS` | 查询应用使用统计，追踪前后台切换 | 特殊权限，用户手动授权 |
| `POST_NOTIFICATIONS` | 前台服务通知 (Android 13+) | 运行时权限 |
| `FOREGROUND_SERVICE` | 前台服务保活监控 | 普通权限 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Android 14 前台服务类型 | 普通权限 |
| `QUERY_ALL_PACKAGES` | 查询已安装应用列表及权限 | 普通权限（需声明合理用途） |

**不申请的权限及原因：**
- `INTERNET` — 本应用不联网，数据纯本地
- `READ/WRITE_EXTERNAL_STORAGE` — 使用 Scoped Storage / MediaStore，不需要
- `ACCESS_FINE/COARSE_LOCATION` — 不需要位置
- `RECORD_AUDIO` — 不监听麦克风
- `CAMERA` — 不使用摄像头

### 3.4 VPN 网络监控（可选模块）

VpnService 模块默认关闭，用户在设置中手动开启。开启后建立本地 VPN Tunnel，将设备流量路由到本应用进程，解析 DNS 请求和 TCP 连接目标，按 App UID 归属网络请求。

**技术要点：**
- 使用 `VpnService.Builder` 建立 VPN Tunnel
- 通过 `/proc/net/` 或 `ConnectivityManager` 关联 UID → 包名
- 解析 DNS 请求（UDP 53）获取域名
- HTTPS 流量只能获取目标 IP/域名，无法解密内容
- 此功能耗电较高，UI 中需显著提醒

---

## 四、功能模块详细设计

### 4.1 模块总览

| # | 模块 | 核心类/服务 | 数据产出 |
|---|------|-----------|---------|
| M1 | 剪贴板监控 | ClipboardMonitorService | ClipboardEvent |
| M2 | 无障碍监控 | PrivacyAccessibilityService | AccessibilityEvent / ClipboardAccessEvent |
| M3 | 通知监控 | NotificationMonitorService | NotificationEvent |
| M4 | 使用统计追踪 | UsageStatsWorker | AppUsageEvent |
| M5 | 权限审计 | PermissionAuditWorker | AppPermissionSnapshot |
| M6 | 网络监控 | NetworkMonitorService (VpnService) | NetworkConnectionEvent |
| M7 | 关联分析引擎 | CorrelationEngine | CorrelationResult |
| M8 | 风险评分 | RiskScorer | AppRiskScore |
| M9 | 数据导出 | ExportManager | JSON/CSV 文件 |

### 4.2 M1 — 剪贴板监控

**职责：** 监听系统剪贴板内容变化，记录变化时间和内容摘要。

**实现方式：**
- 前台 Service 持有 `ClipboardManager.OnPrimaryClipChangedListener`
- 剪贴板变化时，提取内容类型（文本/URI/Intent）和文本前 N 字符摘要
- 内容存储策略：默认仅存内容哈希 + 前 20 字符摘要，用户可选"存全文"

**数据结构：**
```
ClipboardEvent {
    id: Long              // 自增主键
    timestamp: Long       // 事件时间戳
    contentHash: String   // 内容 SHA-256 哈希
    contentPreview: String // 前 20 字符摘要
    contentType: String   // "text" / "uri" / "intent"
    sourceApp: String?    // 来源 App 包名（如可推断）
    readByApps: List<String> // 被哪些 App 读取（由 M2 补充）
}
```

**限制说明：** `OnPrimaryClipChangedListener` 只能感知剪贴板内容被写入（复制/剪切），**无法直接感知被读取（粘贴）**。读取行为需通过 M2 无障碍服务捕获 Android 12+ 的系统 Toast 来补充。

### 4.3 M2 — 无障碍监控

**职责：** 捕获屏幕上的隐私相关事件，重点包括：
1. Android 12+ "XXX 已从剪贴板粘贴" 的系统 Toast → 识别读取剪贴板的 App
2. App 前后台切换 → 记录哪个 App 在何时处于前台
3. 屏幕文本变化 → 可选检测与用户标注的敏感话题相关的内容

**实现方式：**
- 继承 `AccessibilityService`
- 配置 `accessibility-service-config.xml`，监听 `typeWindowStateChanged`、`typeWindowContentChanged`、`typeNotificationStateChanged`
- 过滤系统 Toast 文本（包含"粘贴"/"剪贴板"关键词），通过当前前台 App 包名推断读取方
- 前后台切换通过 `typeWindowStateChanged` 事件中包名变化判断

**accessibility-service-config.xml 关键配置：**
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="200"
    android:packageNames="" />
```

**数据结构：**
```
AccessibilityEvent {
    id: Long
    timestamp: Long
    eventType: String       // "clipboard_access" / "app_foreground" / "app_background"
    packageName: String     // 事件来源 App 包名
    screenTextSnippet: String? // 可选：屏幕文本片段（脱敏后）
    relatedClipboardHash: String? // 关联的剪贴板事件哈希
}
```

**限制说明：**
- 无障碍服务能读取屏幕内容，但仅限于当前前台 App 的可见区域
- 大量 App 使用 Webview 或自绘 UI，无障碍节点可能无法提取文本
- 性能敏感：需做事件节流和过滤，避免高频回调导致卡顿

### 4.4 M3 — 通知监控

**职责：** 捕获所有 App 推送的通知，记录通知内容、来源 App 和时间。这是**最直接的"App 推荐内容证据"** —— 如果淘宝推了"登山套装"通知，就是个性化推荐的确凿信号。

**实现方式：**
- 继承 `NotificationListenerService`
- 重写 `onNotificationPosted(StatusBarNotification)`
- 提取 `Notification.extras` 中的 `EXTRA_TITLE` 和 `EXTRA_TEXT`
- 与用户标注的敏感话题做关键词匹配，标记"疑似关联"通知

**数据结构：**
```
NotificationEvent {
    id: Long
    timestamp: Long
    packageName: String      // 来源 App
    title: String            // 通知标题
    text: String             // 通知正文
    category: String         // 通知类别（如 "promo", "social", "msg"）
    matchedTopics: List<Long> // 匹配到的敏感话题 ID 列表
    isRead: Boolean          // 用户是否已查看
}
```

**存储策略：** 通知正文涉及隐私，默认只存前 50 字符 + 哈希。用户可在设置中开启"全文存储"。

### 4.5 M4 — 使用统计追踪

**职责：** 定期查询应用使用统计，构建各 App 的前后台时间线。

**实现方式：**
- `WorkManager` 周期性任务（每 15 分钟）查询 `UsageStatsManager.queryEvents()`
- 提取 `MOVE_TO_FOREGROUND` 和 `MOVE_TO_BACKGROUND` 事件
- 构建全天候 App 使用时间线

**数据结构：**
```
AppUsageEvent {
    id: Long
    timestamp: Long
    packageName: String
    eventType: String    // "foreground" / "background"
    durationMs: Long?    // 前台持续时长（background 事件时计算）
}
```

### 4.6 M5 — 权限审计

**职责：** 扫描所有已安装 App 的权限声明，标记敏感权限持有者。

**实现方式：**
- `WorkManager` 周期性任务（每天一次）+ 应用安装/卸载时触发
- `PackageManager.getInstalledApplications()` 获取所有 App
- 对每个 App 调用 `getPackageInfo(pkg, GET_PERMISSIONS)` 获取权限清单
- 分类标记敏感权限：位置、麦克风、摄像头、通讯录、剪贴板（隐式）、存储等
- 可选：通过反射调用 `AppOpsManager.checkOp()` 查询操作记录

**数据结构：**
```
AppPermissionSnapshot {
    id: Long
    timestamp: Long
    packageName: String
    appName: String
    sensitivePermissions: List<PermissionInfo>
    isSystemApp: Boolean
    installerPackage: String? // 安装来源
}

PermissionInfo {
    name: String         // 权限名
    isGranted: Boolean   // 是否已授予
    sensitivityLevel: Int // 1=低 2=中 3=高
    category: String     // "location" / "microphone" / "camera" / "contacts" / "storage" / "clipboard"
}
```

### 4.7 M6 — 网络监控（可选）

**职责：** 通过 VpnService 捕获各 App 的网络连接目标（域名/IP），帮助用户理解数据流向。

**实现要点：**
- 独立 VpnService 子类
- VPN Tunnel 接收所有上行流量
- DNS 请求解析：提取 Query 域名，按 UID 归属 App
- TCP/UDP 连接：记录目标 IP:Port，按 UID 归属
- 不解密 HTTPS 内容（也无法解密）

**数据结构：**
```
NetworkConnectionEvent {
    id: Long
    timestamp: Long
    uid: Int               // App UID
    packageName: String    // 由 UID 反查
    protocol: String       // "DNS" / "TCP" / "UDP"
    targetHost: String     // 域名或 IP
    targetPort: Int?
    bytesOut: Long
    bytesIn: Long
}
```

### 4.8 M7 — 关联分析引擎

**职责：** 这是整个 App 的"大脑"——将分散的监控信号串联成有意义的隐私洞察。

**核心概念：敏感事件标注**

用户在 App 中手动创建一个"敏感事件"：
```
SensitiveEvent {
    id: Long
    timestamp: Long        // 事件发生时间
    title: String          // 如 "与朋友谈论爬山"
    description: String    // 详细描述
    keywords: List<String> // ["爬山", "登山", "徒步"]
    scope: String          // "offline_conversation" / "online_search" / "clipboard_copy" / "custom"
}
```

**关联算法：**

给定一个敏感事件 S（时间 T，关键词集 K），系统执行以下关联：

1. **剪贴板关联**：检查 T 前后 30 分钟内是否有包含 K 中关键词的剪贴板事件
2. **通知关联**：检查 T 后 24 小时内是否有通知内容匹配 K 中关键词的推送（如"登山套装""旅游攻略"）
3. **App 行为关联**：检查 T 前后哪些 App 有异常的前后台切换、剪贴板读取行为
4. **网络关联**：检查 T 前后哪些 App 连接了广告/追踪类域名

**关联结果：**
```
CorrelationResult {
    sensitiveEventId: Long
    relatedEvents: List<RelatedEvent>
    confidenceScore: Float     // 0.0 - 1.0，关联可信度
    summary: String            // 自然语言摘要
}

RelatedEvent {
    eventType: String  // "notification" / "clipboard" / "app_usage" / "network"
    timestamp: Long
    packageName: String
    matchedContent: String
    matchReason: String // 为什么认为是关联的
}
```

**诚实声明：** 关联分析基于时间和关键词匹配，是**统计推断而非因果关系证明**。系统会在结果中标注"疑似关联"而非"已确认"。App 内会有明确提示，避免用户误解。

### 4.9 M8 — 风险评分

**职责：** 为每个 App 计算隐私风险评分，帮助用户快速识别高风险 App。

**评分模型（加权打分）：**

| 维度 | 权重 | 评分依据 |
|------|------|---------|
| 敏感权限数量 | 30% | 高危权限（麦克风/位置/通讯录）数量 × 权重 |
| 剪贴板读取频率 | 20% | 单位时间内读取剪贴板次数 |
| 通知关联命中率 | 20% | 通知内容匹配敏感事件的比率 |
| 后台活跃度 | 15% | 后台运行时长占比 |
| 网络追踪域名 | 15% | 连接已知广告/追踪域名的比例 |

**评分输出：**
```
AppRiskScore {
    packageName: String
    appName: String
    totalScore: Int        // 0-100，越高越危险
    level: String          // "low" / "medium" / "high" / "critical"
    breakdown: Map<String, Int> // 各维度得分
    lastUpdated: Long
}
```

### 4.10 M9 — 数据导出

**职责：** 支持将监控数据导出为 JSON/CSV 文件供用户备份或分析。

- 导出范围可选：全部数据 / 指定时间范围 / 指定 App
- 导出格式：JSON（完整结构化）+ CSV（事件列表）
- 导出文件通过 `FileProvider` + `ACTION_SEND` 共享

---

## 五、数据库设计

### 5.1 Room 数据库 Schema

```kotlin
// 数据库版本 1，包含以下实体

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    val riskScore: Int = 0,
    val riskLevel: String = "unknown",
    val lastAudited: Long? = null
)

@Entity(tableName = "clipboard_events")
data class ClipboardEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val contentHash: String,
    val contentPreview: String,
    val contentType: String,
    val sourceApp: String?
)

@Entity(tableName = "clipboard_access_events")
data class ClipboardAccessEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val clipboardContentHash: String?
)

@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val title: String,
    val text: String,
    val category: String?,
    val matchedTopicIds: String? // CSV 格式的 topic ID
)

@Entity(tableName = "app_usage_events")
data class AppUsageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val eventType: String,
    val durationMs: Long?
)

@Entity(tableName = "permission_snapshots")
data class PermissionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val permissionsJson: String // JSON 序列化的权限列表
)

@Entity(tableName = "network_events")
data class NetworkEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val uid: Int,
    val packageName: String,
    val protocol: String,
    val targetHost: String,
    val targetPort: Int?,
    val bytesOut: Long = 0,
    val bytesIn: Long = 0
)

@Entity(tableName = "sensitive_events")
data class SensitiveEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val title: String,
    val description: String,
    val keywords: String, // CSV 格式
    val scope: String
)

@Entity(tableName = "correlation_results")
data class CorrelationResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensitiveEventId: Long,
    val resultJson: String, // 完整关联结果 JSON
    val confidenceScore: Float,
    val createdAt: Long
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    val value: String
)
// 用于存储应用配置：主题模式、存储策略、各服务开关状态等
```

### 5.2 索引设计

```kotlin
// ClipboardEventEntity
@Index("timestamp")
@Index("contentHash")

// NotificationEventEntity
@Index("timestamp")
@Index("packageName")

// AppUsageEventEntity
@Index("timestamp")
@Index("packageName")

// NetworkEventEntity
@Index("timestamp")
@Index("packageName")

// SensitiveEventEntity
@Index("timestamp")
```

### 5.3 数据清理策略

- 默认保留 30 天数据，超期自动清理
- 用户可调整保留天数（7/14/30/90天/永久）
- 清理工作由 `WorkManager` 每日执行
- 清理前可选提示用户导出备份

---

## 六、UI/UX 设计

### 6.1 设计语言

- **Material 3 (Material You)** 设计系统
- **主题色跟随系统**：Android 12+ 使用 `dynamicColorScheme`，Android 10-11 使用自定义主题色但支持深色/浅色跟随系统
- **无广告**：纯工具界面，信息密度高
- **导航**：底部 Navigation Bar，5 个 Tab

### 6.2 页面结构

```
BigDataMonitor
├── 仪表盘 (Dashboard)     — 首页，隐私事件总览
├── 时间线 (Timeline)      — 按时间排列所有监控事件
├── 应用列表 (AppList)     — 所有已安装 App 的隐私风险评分
├── 关联分析 (Correlation) — 敏感事件标注 + 关联结果
└── 设置 (Settings)        — 主题、权限、存储、导出
```

### 6.3 各页面详细设计

#### 6.3.1 仪表盘 (Dashboard)

**布局：** 顶部摘要卡片 + 中部风险排行 + 底部今日事件摘要

```
┌──────────────────────────────────┐
│  BigDataMonitor          [盾牌图标] │
├──────────────────────────────────┤
│                                    │
│  今日隐私事件：12                  │
│  剪贴板被读取：3 次                │
│  疑似关联通知：2 条                │
│  高风险应用：1 个                  │
│                                    │
│  ┌──────┐ ┌──────┐ ┌──────┐     │
│  │ 剪贴板 │ │ 通知  │ │ 网络  │     │
│  │  3   │ │  5   │ │  4   │     │
│  └──────┘ └──────┘ └──────┘     │
│                                    │
│  ▸ 隐私风险排行                    │
│  🔴 淘宝      82 分  高风险        │
│  🟡 抖音      61 分  中风险        │
│  🟢 微信      28 分  低风险        │
│                                    │
│  ▸ 今日事件时间线（缩略）           │
│  14:32  淘宝读取剪贴板             │
│  15:01  抖音推送"登山攻略"         │
│  ...                              │
│                                    │
└──────────────────────────────────┘
```

#### 6.3.2 时间线 (Timeline)

- 垂直时间线，按时间倒序排列
- 事件类型用不同颜色图标区分（剪贴板=蓝、通知=橙、使用=绿、网络=紫）
- 支持 filter chip：按事件类型、按 App 过滤
- 每条事件可展开查看详情
- 长按事件可"标记为关联敏感事件"

#### 6.3.3 应用列表 (AppList)

- 列表展示所有已安装 App
- 按 风险评分 降序排列
- 每个 App 显示：图标、名称、风险评分、敏感权限数、今日事件数
- 点击进入 App 详情页

**App 详情页：**
- App 基本信息与安装来源
- 持有的敏感权限清单（分类展示）
- 该 App 的事件时间线
- 该 App 的网络连接目标（如开启 VPN 监控）
- 风险评分明细

#### 6.3.4 关联分析 (Correlation)

**功能：** 用户标注敏感事件 → 系统执行关联分析 → 展示关联结果

```
┌──────────────────────────────────┐
│  关联分析                          │
├──────────────────────────────────┤
│                                    │
│  [+ 标注敏感事件]                  │
│                                    │
│  📌 已标注事件（2）                │
│  ┌──────────────────────────────┐ │
│  │ 14:00 与朋友谈论爬山           │ │
│  │ 关键词：爬山, 登山, 徒步        │ │
│  │ 关联结果：3 条疑似关联          │ │
│  │                                │ │
│  │  ▸ 14:05 剪贴板内容变化         │ │
│  │    内容包含"爬山"               │ │
│  │  ▸ 15:30 淘宝推送"登山套装"     │ │
│  │    匹配关键词"登山"             │ │
│  │  ▸ 16:00 抖音推送"登山攻略"     │ │
│  │    匹配关键词"登山"             │ │
│  │                                │ │
│  │ 可信度：71%（中）               │ │
│  │ ⚠ 关联为推断，非因果证明        │ │
│  └──────────────────────────────┘ │
│                                    │
└──────────────────────────────────┘
```

#### 6.3.5 设置 (Settings)

```
设置
├── 外观
│   ├── 主题色跟随系统 [开关] ← 核心需求
│   ├── 深色模式（跟随系统/强制开/强制关）
│   └── 动态取色（Android 12+）[开关]
├── 监控服务
│   ├── 剪贴板监控 [开关]
│   ├── 通知监控 [开关]
│   ├── 使用统计追踪 [开关]
│   ├── 权限审计 [开关]
│   └── 网络监控（VPN）[开关] + 耗电提醒
├── 数据存储
│   ├── 数据保留天数（7/14/30/90/永久）
│   ├── 通知内容存储（摘要/全文）
│   ├── 剪贴板内容存储（摘要/全文）
│   └── 立即清理数据
├── 数据导出
│   ├── 导出为 JSON
│   ├── 导出为 CSV
│   └── 导出范围选择
├── 关于
│   ├── 版本信息
│   ├── 隐私声明（不联网、不上传）
│   ├── 技术限制说明
│   └── 开源许可
```

### 6.4 首次启动引导

首次启动时展示 3 页引导：
1. **欢迎页**：介绍应用目标，声明"不联网、不上传、无广告"
2. **权限授权页**：引导用户依次开启无障碍服务、通知监听、使用统计权限
3. **能力与限制说明页**：诚实告知用户能监控什么、不能监控什么，管理预期

---

## 七、关键实现细节

### 7.1 前台服务设计

- 使用一个主前台 Service（`PrivacyMonitorService`）持有剪贴板监听器
- 前台通知显示监控状态（"正在监控隐私事件"），提供快捷暂停按钮
- `NotificationListenerService` 和 `AccessibilityService` 是系统绑定的独立服务，不需要额外前台化
- `WorkManager` 周期任务负责使用统计和权限审计

### 7.2 无障碍服务事件节流

无障碍服务会产生大量事件，必须做节流：
- 对 `typeWindowContentChanged` 做 500ms debounce
- 只处理 `packageName` 非系统 UI 的事件
- 剪贴板 Toast 检测：匹配 `typeNotificationStateChanged` 事件中包含"粘贴"/"剪贴板"文本的 Toast
- 在 `accessibility-service-config.xml` 中设置 `notificationTimeout="200"`

### 7.3 通知内容关键词匹配

- 用户标注敏感事件时填入关键词
- 系统维护关键词索引（Room 查询）
- 每条通知入库时，与所有活跃关键词做子串匹配
- 匹配命中则在 `matchedTopicIds` 中记录关联

### 7.4 主题色跟随系统实现

```kotlin
// Android 12+ (API 31+)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 跟随系统开关
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- 设置开关 `dynamicColor` 存储在 DataStore 中
- Android 10-11 设备上，`dynamicColor` 开关不可用（灰显），使用预设主题色

### 7.5 数据导出实现

```kotlin
// 导出为 JSON
suspend fun exportToJson(range: ExportRange): Uri {
    val data = ExportData(
        apps = appDao.getAll(),
        clipboardEvents = clipboardDao.getByRange(range.start, range.end),
        notificationEvents = notificationDao.getByRange(range.start, range.end),
        // ... 其他数据
    )
    val json = Json.encodeToString(data)
    val file = File(context.cacheDir, "bigdatamonitor_export_${System.currentTimeMillis()}.json")
    file.writeText(json)
    return FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)
}
```

---

## 八、技术限制与诚实声明

本应用在首次启动和技术限制说明页中明确告知以下内容：

> **重要声明**
>
> BigDataMonitor 是一款基于 Android 合法 API 的隐私行为感知工具。由于 Android 安全沙箱的限制，本应用存在以下固有限制：
>
> 1. **无法直接监听其他 App 的麦克风采集行为**。应用只能告诉你哪个 App 拥有麦克风权限，以及该 App 何时在前台/后台运行，但无法确认它是否在某个时刻实际启动了麦克风。
> 2. **无法截获 App 之间的数据共享**。如果 App A 通过 SDK 将数据共享给 App B，本应用无法直接检测。你可以通过网络监控观察各 App 连接了哪些追踪/广告域名来间接推断。
> 3. **关联分析是统计推断，非因果证明**。当你标注"14:00 谈论爬山"后，系统发现 15:00 淘宝推送了登山装备，这只能说明存在时间上的关联，不能确认淘宝确实获取了你的对话数据。真实原因可能是巧合、其他渠道的数据关联、或你的搜索行为。
> 4. **剪贴板读取检测在 Android 12+ 效果较好**。Android 12 引入了剪贴板访问提示，本应用可通过无障碍服务捕获。Android 10-11 设备无法检测哪个 App 读取了剪贴板。
> 5. **网络监控为可选项，耗电较高**。VPN 模式会接管设备所有流量，可能影响网络速度和电池续航。
>
> 本应用的目标是**让隐形的隐私行为变为可见**，提供信号和线索供你判断，而非提供一个完美的隐私监控解决方案。

---

## 九、开发里程碑

| 阶段 | 内容 | 产出 |
|------|------|------|
| P0 | 工程脚手架：Gradle 工程、主题系统、导航、Room 数据库 | 可编译运行的空壳 App |
| P1 | M1 剪贴板监控 + M3 通知监控 + M4 使用统计 | 三条核心数据采集线 |
| P2 | M5 权限审计 + Dashboard/AppList 页面 | 可展示 App 权限和风险评分 |
| P3 | M2 无障碍服务 + Timeline 页面 | 剪贴板读取检测 + 事件时间线 |
| P4 | M7 关联分析引擎 + Correlation 页面 | 敏感事件标注与关联分析 |
| P5 | M8 风险评分优化 + M9 数据导出 | 风险评分模型 + 导出功能 |
| P6 | M6 网络监控（VpnService） | 可选网络流量监控 |
| P7 | 首次引导、技术限制说明、打磨 | 可发布版本 |

---

## 十、项目结构

```
BigDataMonitor/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/bigdatamonitor/
│           │   ├── BigDataMonitorApp.kt          # Application + Hilt
│           │   ├── MainActivity.kt               # Compose 入口
│           │   ├── di/
│           │   │   ├── DatabaseModule.kt
│           │   │   └── RepositoryModule.kt
│           │   ├── data/
│           │   │   ├── db/
│           │   │   │   ├── AppDatabase.kt
│           │   │   │   ├── entity/               # Room 实体
│           │   │   │   └── dao/                  # Room DAO
│           │   │   ├── repository/               # Repository 实现
│           │   │   └── datastore/                # DataStore 配置
│           │   ├── domain/
│           │   │   ├── model/                    # 领域模型
│           │   │   ├── CorrelationEngine.kt
│           │   │   ├── RiskScorer.kt
│           │   │   └── PrivacyAnalyzer.kt
│           │   ├── service/
│           │   │   ├── PrivacyMonitorService.kt  # 前台服务
│           │   │   ├── PrivacyAccessibilityService.kt
│           │   │   ├── NotificationMonitorService.kt
│           │   │   ├── NetworkMonitorService.kt  # VpnService
│           │   │   └── ClipboardManagerHelper.kt
│           │   ├── worker/
│           │   │   ├── UsageStatsWorker.kt
│           │   │   ├── PermissionAuditWorker.kt
│           │   │   └── DataCleanupWorker.kt
│           │   ├── ui/
│           │   │   ├── theme/                    # Material 3 主题
│           │   │   ├── navigation/               # 导航
│           │   │   ├── dashboard/                # 仪表盘
│           │   │   ├── timeline/                 # 时间线
│           │   │   ├── applist/                  # 应用列表
│           │   │   ├── correlation/             # 关联分析
│           │   │   ├── settings/                 # 设置
│           │   │   ├── onboarding/              # 首次引导
│           │   │   └── components/              # 通用组件
│           │   ├── util/
│           │   │   ├── PermissionUtil.kt
│           │   │   ├── HashUtil.kt
│           │   │   ├── ExportManager.kt
│           │   │   └── AppInfoUtil.kt
│           │   └── receiver/
│           │       └── PackageChangeReceiver.kt
│           └── res/
│               ├── xml/
│               │   ├── accessibility_service_config.xml
│               │   ├── vpn_config.xml
│               │   └── file_paths.xml
│               ├── values/
│               │   ├── strings.xml
│               │   ├── colors.xml
│               │   └── themes.xml
│               ├── values-night/
│               │   └── themes.xml
│               ├── drawable/
│               └── mipmap/
```

---

## 附录 A：敏感权限分类表

| 类别 | 权限 | 敏感度 |
|------|------|--------|
| 位置 | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, ACCESS_BACKGROUND_LOCATION | 高 |
| 麦克风 | RECORD_AUDIO | 高 |
| 摄像头 | CAMERA | 高 |
| 通讯录 | READ_CONTACTS, WRITE_CONTACTS | 高 |
| 通话 | READ_CALL_LOG, READ_PHONE_STATE, CALL_PHONE | 高 |
| 短信 | READ_SMS, SEND_SMS, RECEIVE_SMS | 高 |
| 存储 | READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE | 中 |
| 剪贴板 | (隐式访问，无显式权限) | 中 |
| 网络 | INTERNET, ACCESS_NETWORK_STATE | 低（但用于数据上传） |
| 日历 | READ_CALENDAR, WRITE_CALENDAR | 中 |
| 传感器 | BODY_SENSORS, ACTIVITY_RECOGNITION | 中 |
| 通知 | POST_NOTIFICATIONS (Android 13+) | 低 |

## 附录 B：已知广告/追踪域名参考列表

用于网络监控模块中标记网络连接的性质：

```
# 广告网络
doubleclick.net
googlesyndication.com
googleadservices.com
adservice.google.com
facebook.com/tr
ads.yahoo.com

# 分析追踪
google-analytics.com
googletagmanager.com
flurry.com
mixpanel.com
amplitude.com
adjust.com
appsflyer.com
sensors.com
sensorsdata.cn

# 国内广告/追踪
umeng.com
umeng.co
umengcloud.com
tanx.com
alimama.com
pdd.com
```

> AI生成