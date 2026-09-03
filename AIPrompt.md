
# AI 开发提示词 — BigDataMonitor 隐私数据监控 App

## 角色与职责

你是一位资深 Android 开发工程师，精通 Kotlin、Jetpack Compose、Material 3 设计体系和 Android 系统服务。你需要根据下方设计方案，从零开发一款完整的、可编译运行的 Android 应用 **BigDataMonitor**。

项目的完整设计方案在同目录的 `DesignDocument.md` 中，你**必须先完整阅读该方案**再开始编码。本提示词是对方案的补充约束和编码指令。

---

## 一、项目基本信息

| 项 | 值 |
|---|---|
| 应用名称 | BigDataMonitor |
| 包名 | com.bigdatamonitor |
| 最低 SDK | API 29 (Android 10) |
| 目标 SDK | API 34 (Android 14) |
| 编译 SDK | API 34 |
| 语言 | Kotlin 1.9+ |
| JDK | 17 |
| 构建工具 | Gradle 8.x (Kotlin DSL) |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | 单 Activity + Compose Navigation + MVVM |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 后台任务 | WorkManager |
| 偏好存储 | DataStore (Preferences) |

---

## 二、硬性约束（必须遵守）

### 2.1 不申请 INTERNET 权限
应用**不得**在 AndroidManifest.xml 中声明 `<uses-permission android:name="android.permission.INTERNET" />`，也不得声明 `ACCESS_NETWORK_STATE`（网络监控模块通过 VpnService 实现而非网络权限）。应用是完全离线的。

### 2.2 无广告、无内购、无分析 SDK
不得引入任何广告 SDK、统计分析 SDK、崩溃上报 SDK 或任何需要联网的第三方库。所有依赖必须是纯本地功能库。

### 2.3 最小权限原则
仅申请设计方案 3.3 节中列出的权限，不多申请任何额外权限。

### 2.4 主题色跟随系统
必须实现一个设置开关，控制是否使用 Material You 动态取色。开启时（Android 12+）应用主题色跟随系统壁纸取色；关闭时使用预设主题色。深色/浅色模式始终跟随系统设置。

### 2.5 数据纯本地存储
所有监控数据仅存储在应用内部 Room 数据库中，不上传任何数据到任何服务器。

### 2.6 诚实声明
应用必须在首次启动时展示"技术限制说明页"，向用户如实告知应用的能力边界（详见设计方案第八节）。

### 2.7 代码完整可编译
所有代码文件必须完整、无占位符、无 TODO 标记、无 `// 在此实现` 之类的省略。每个类、方法、字段都必须有完整实现。Gradle 工程结构完整，可直接用 Android Studio 打开编译。

---

## 三、依赖清单

在 `app/build.gradle.kts` 中使用以下依赖：

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")

// Hilt
implementation("com.google.dagger:hilt-android:2.50")
kapt("com.google.dagger:hilt-android-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
implementation("androidx.hilt:hilt-work:1.2.0")
kapt("androidx.hilt:hilt-compiler:1.2.0")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Serialization (用于导出 JSON)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
```

**注意：**
- 不引入任何网络库（Retrofit、OkHttp 等）
- 不引入任何广告/分析 SDK
- 使用 kapt 而非 ksp（确保兼容性）

---

## 四、需要实现的模块与优先级

按以下顺序实现各模块。每个模块必须是完整可用的代码，不能只写接口不写实现。

### 第一批：工程基础（P0）

1. **Gradle 工程结构**：`settings.gradle.kts`、根 `build.gradle.kts`、`app/build.gradle.kts`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties`
2. **AndroidManifest.xml**：声明所有权限、Service、Receiver、Application
3. **Application 类**：`BigDataMonitorApp.kt`，初始化 Hilt
4. **主题系统**：`theme/` 目录，包括 `Color.kt`、`Theme.kt`、`Type.kt`，实现 dynamicColor 开关
5. **导航**：底部 5 Tab 导航
6. **Room 数据库**：所有实体类、DAO 接口、Database 类
7. **DataStore**：应用配置存储（主题开关、各服务开关、存储策略等）

### 第二批：数据采集服务（P1）

8. **剪贴板监控服务**：`PrivacyMonitorService.kt`（前台服务 + ClipboardManager 监听）
9. **通知监听服务**：`NotificationMonitorService.kt`
10. **使用统计 WorkManager**：`UsageStatsWorker.kt`
11. **权限审计 WorkManager**：`PermissionAuditWorker.kt`
12. **数据清理 WorkManager**：`DataCleanupWorker.kt`

### 第三批：无障碍服务与关联分析（P2-P4）

13. **无障碍服务**：`PrivacyAccessibilityService.kt` + `accessibility_service_config.xml`
14. **关联分析引擎**：`CorrelationEngine.kt`
15. **风险评分引擎**：`RiskScorer.kt`
16. **PackageChangeReceiver**：监听应用安装/卸载触发权限审计

### 第四批：UI 页面（全程）

17. **首次引导页**：3 页引导（欢迎、权限授权、技术限制说明）
18. **仪表盘页面**：隐私事件总览
19. **时间线页面**：事件列表 + 过滤
20. **应用列表页面** + App 详情页
21. **关联分析页面**：敏感事件标注 + 关联结果展示
22. **设置页面**：所有配置项

### 第五批：高级功能（P5-P6）

23. **数据导出**：`ExportManager.kt` + FileProvider 配置
24. **网络监控 VpnService**（可选模块）：`NetworkMonitorService.kt` + DNS 解析逻辑

---

## 五、各模块编码要求

### 5.1 AndroidManifest.xml

必须包含以下声明：

```xml
<!-- 权限 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />

<!-- 注意：不申请 INTERNET 和 ACCESS_NETWORK_STATE -->

<!-- 前台服务 -->
<service android:name=".service.PrivacyMonitorService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Privacy monitoring - clipboard and usage tracking" />
</service>

<!-- 无障碍服务 -->
<service android:name=".service.PrivacyAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>

<!-- 通知监听服务 -->
<service android:name=".service.NotificationMonitorService"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>

<!-- VPN 服务 -->
<service android:name=".service.NetworkMonitorService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>

<!-- Receiver -->
<receiver android:name=".receiver.PackageChangeReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.PACKAGE_ADDED" />
        <action android:name="android.intent.action.PACKAGE_REMOVED" />
        <data android:scheme="package" />
    </intent-filter>
</receiver>

<!-- FileProvider -->
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 5.2 主题系统

`Theme.kt` 中实现：

```kotlin
@Composable
fun BigDataMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // 由 DataStore 读取
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BigDataMonitorTypography,
        content = content
    )
}
```

预设配色方案（Android 10-11 回退用）：

- LightColors：主色 `#2E7D32`（绿色，象征隐私保护），次色 `#FF6F00`
- DarkColors：主色 `#81C784`，次色 `#FFB74D`

### 5.3 数据库

- 所有实体类放在 `data/db/entity/` 下
- 所有 DAO 接口放在 `data/db/dao/` 下
- `AppDatabase.kt` 中注册所有实体和 DAO
- 版本号初始为 1
- 所有时间戳用 `Long`（毫秒级 epoch）
- DAO 查询返回 `Flow<List<...>>` 或 `suspend fun`

### 5.4 剪贴板监控服务

```kotlin
class PrivacyMonitorService : Service() {
    // 关键点：
    // 1. onCreate 中创建前台通知，调用 startForeground()
    // 2. 注册 ClipboardManager.OnPrimaryClipChangedListener
    // 3. 监听器中提取剪贴板内容，计算 SHA-256 哈希，截取前 20 字符摘要
    // 4. 写入 Room 数据库 (通过 Hilt 注入 Repository)
    // 5. 前台通知显示"正在监控隐私事件"，带暂停按钮 (ACTION_STOP)
    // 6. onStartCommand 处理 ACTION_STOP，调用 stopSelf()
    // 7. onDestroy 中注销监听器
    // 8. 确保服务在 Android 14 上正确声明 foregroundServiceType="specialUse"
}
```

### 5.5 通知监听服务

```kotlin
class NotificationMonitorService : NotificationListenerService() {
    // 关键点：
    // 1. 重写 onNotificationPosted(StatusBarNotification)
    // 2. 从 notification.extras 提取 EXTRA_TITLE 和 EXTRA_TEXT
    // 3. 默认只存前 50 字符 + 内容哈希
    // 4. 与当前所有活跃 SensitiveEvent 的 keywords 做匹配
    // 5. 匹配命中则记录 matchedTopicIds
    // 6. 使用 Hilt注入 Repository (通过 @AndroidEntryPoint 或手动获取)
    // 7. 注意：NotificationListenerService 不能直接用 @AndroidEntryPoint
    //    需要通过 EntryPointAccessors 获取依赖
}
```

### 5.6 无障碍服务

```kotlin
class PrivacyAccessibilityService : AccessibilityService() {
    // 关键点：
    // 1. 重写 onAccessibilityEvent(AccessibilityEvent)
    // 2. 处理 typeWindowStateChanged: 检测前后台 App 切换
    // 3. 处理 typeWindowContentChanged: 做 500ms debounce，过滤高频事件
    // 4. 检测 Android 12+ 剪贴板 Toast:
    //    - 事件文本包含"粘贴"/"剪贴板"/"pasted"等关键词
    //    - 记录当前前台 App 包名作为读取方
    // 5. 过滤掉系统 UI (com.android.systemui) 的事件（Toast 除外）
    // 6. 所有事件写入数据库
    // 7. onServiceConnected 中做必要初始化
}
```

### 5.7 使用统计 Worker

```kotlin
class UsageStatsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    // 关键点：
    // 1. 检查 PACKAGE_USAGE_STATS 权限 (UsageStatsManager.checkAppOp())
    // 2. 如果无权限，返回 Result.success() 不报错（静默跳过）
    // 3. queryEvents(now - 15min, now) 获取最近 15 分钟事件
    // 4. 过滤 MOVE_TO_FOREGROUND 和 MOVE_TO_BACKGROUND 事件
    // 5. 写入数据库
    // 6. 周期性任务，每 15 分钟执行一次
}
```

### 5.8 权限审计 Worker

```kotlin
class PermissionAuditWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    // 关键点：
    // 1. 获取所有已安装应用 PackageManager.getInstalledApplications()
    // 2. 逐个查询权限: getPackageInfo(pkg, GET_PERMISSIONS)
    // 3. 与敏感权限分类表对比，标记敏感权限
    // 4. 写入 permission_snapshots 表
    // 5. 更新 apps 表的 riskScore 和 riskLevel
    // 6. 调用 RiskScorer 计算评分
}
```

### 5.9 关联分析引擎

```kotlin
class CorrelationEngine @Inject constructor(
    private val clipboardDao: ClipboardEventDao,
    private val notificationDao: NotificationEventDao,
    private val usageDao: AppUsageEventDao,
    private val networkDao: NetworkEventDao,
    private val correlationDao: CorrelationResultDao
) {
    // 核心方法：
    suspend fun analyze(sensitiveEvent: SensitiveEventEntity): CorrelationResultEntity {
        // 1. 剪贴板关联：查找 timestamp 前后 30 分钟内、contentPreview 包含任一 keyword 的事件
        // 2. 通知关联：查找 timestamp 后 24 小时内、title 或 text 包含任一 keyword 的通知
        // 3. App 使用关联：查找 timestamp 前后 1 小时内异常活跃的 App
        // 4. 网络关联：查找 timestamp 前后连接广告/追踪域名的 App
        // 5. 综合 4 条线索计算 confidenceScore
        // 6. 生成自然语言摘要 summary
        // 7. 持久化到 correlation_results 表
    }

    // confidenceScore 计算：
    // - 通知匹配命中: +0.35 per hit (上限 0.7)
    // - 剪贴板匹配命中: +0.2
    // - 网络追踪域名: +0.15
    // - App 活跃异常: +0.1
    // - 上限 1.0
}
```

### 5.10 风险评分引擎

```kotlin
class RiskScorer @Inject constructor(
    private val permissionDao: PermissionSnapshotDao,
    private val clipboardAccessDao: ClipboardAccessEventDao,
    private val notificationDao: NotificationEventDao,
    private val usageDao: AppUsageEventDao,
    private val networkDao: NetworkEventDao
) {
    // 评分模型（详见设计方案 4.9 节）：
    // - 敏感权限数量 30%
    // - 剪贴板读取频率 20%
    // - 通知关联命中率 20%
    // - 后台活跃度 15%
    // - 网络追踪域名 15%
    // 输出 0-100 分和 "low"/"medium"/"high"/"critical" 等级
    // 分界线：0-29 low, 30-59 medium, 60-84 high, 85-100 critical
}
```

### 5.11 UI 页面通用要求

- 所有页面使用 Compose 实现，不使用 XML 布局
- ViewModel 使用 Hilt 注入，状态用 StateFlow
- 列表使用 LazyColumn
- 每个页面包含独立的 ViewModel 和 Screen composable
- 使用 Material 3 组件：TopAppBar、Card、ListItem、FilterChip、Switch、Slider 等
- 空状态要有占位提示文案
- 加载状态用 CircularProgress

### 5.12 仪表盘页面

```
DashboardScreen:
- 顶部 App 标题栏 + 监控状态指示器
- 摘要卡片：今日事件数、剪贴板读取数、疑似关联通知数、高风险应用数
- 三色统计卡片：剪贴板 / 通知 / 网络（点击跳转对应 Timeline 过滤）
- 隐私风险排行 Top 5（点击跳转 App 详情）
- 今日事件时间线缩略（最新 5 条，点击跳转完整 Timeline）
- 监控服务状态卡片：显示各服务运行状态，未开启的跳转设置页
```

### 5.13 时间线页面

```
TimelineScreen:
- 顶部 FilterChip 行：全部 / 剪贴板 / 通知 / 使用 / 网络 / 关联
- App 过滤下拉
- LazyColumn 展示事件
- 每条事件：时间 + 图标 + 事件摘要 + 来源 App
- 点击展开详情 BottomSheet
- 长按弹出菜单：标记为敏感事件关联 / 复制
```

### 5.14 关联分析页面

```
CorrelationScreen:
- 顶部"标注敏感事件"按钮 → 弹出 BottomSheet 表单
- 表单字段：标题、描述、关键词（逗号分隔）、时间（日期时间选择器）、范围选择
- 已标注事件列表（倒序）
- 每条事件卡片：标题 + 时间 + 关键词 + 关联结果数量 + 可信度
- 展开关联结果：列出所有 relatedEvents，每个显示类型 + 时间 + App + 匹配内容 + 匹配原因
- 可信度用进度条 + 颜色表示（绿/黄/红）
- 底部固定提示："⚠ 关联为推断分析，非因果证明"
```

### 5.15 设置页面

```
SettingsScreen:
- 外观组：主题色跟随系统 [Switch]、动态取色 [Switch, Android 12+]、深色模式 [Segmented]
- 监控服务组：各服务开关 + 状态指示 + 权限检查
- 数据存储组：保留天数 [Segmented]、通知存储策略 [Segmented]、剪贴板存储策略 [Segmented]、立即清理 [Button + 确认对话框]
- 数据导出组：导出 JSON、导出 CSV、导出范围选择
- 关于组：版本、隐私声明、技术限制说明、开源许可
```

### 5.16 首次引导

```
OnboardingScreen:
- 3 页 Pager:
  1. 欢迎页：应用介绍 + "不联网、不上传、无广告" 声明 + 开始按钮
  2. 权限授权页：依次列出需要的权限（无障碍、通知监听、使用统计）、每个带"去开启"按钮、跳转到系统设置
  3. 技术限制说明页：如实告知能力边界（设计方案第八节内容）、用户勾选"我已了解"后完成引导
- 完成后写入 DataStore 标记已完成引导
```

### 5.17 网络监控 VpnService

```kotlin
class NetworkMonitorService : VpnService() {
    // 关键点：
    // 1. Builder 建立到本地地址的 VPN Tunnel
    // 2. 创建一个工作线程处理网络数据包
    // 3. 解析 DNS 请求（UDP 53）提取域名
    // 4. 解析 TCP/UDP 连接目标 IP:Port
    // 5. 通过 /proc/net/tcp 或 ConnectivityManager 关联 UID → 包名
    // 6. 已知广告/追踪域名列表做标记（见设计方案附录 B）
    // 7. 写入数据库
    // 8. 开启时显示前台通知 + 暂停按钮
    // 9. 技术复杂度高，可以简化实现：
    //    - 只解析 DNS 请求（域名维度）即可
    //    - TCP 连接可只记录目标 IP
    // 10. 此模块为可选，默认关闭
}
```

---

## 六、编码规范

### 6.1 代码风格
- 所有类、方法添加 KDoc 注释（中文）
- 变量命名使用 camelCase
- 常量命名使用 UPPER_SNAKE_CASE
- 包名全小写
- 每个文件顶部不需要文件头注释
- 使用 `@Inject` 构造函数注入，不用字段注入

### 6.2 错误处理
- 所有数据库操作使用 `try-catch`，异常时记录日志但不崩溃
- Service 中操作数据库使用协程 `CoroutineScope(Dispatchers.IO)`
- 无障碍服务回调中不做耗时操作，必要时 post 到 IO 线程

### 6.3 性能
- 无障碍事件做 debounce（500ms）
- Room 查询确保有索引
- 时间线分页加载（Limit + Offset）
- 避免在 Composable 中直接访问数据库

### 6.4 安全
- 所有用户数据仅存在 Room 数据库中
- 导出文件放入 `cacheDir`，通过 FileProvider 临时授权
- 不在日志中打印剪贴板或通知的完整内容

---

## 七、项目目录结构

```
app/src/main/java/com/bigdatamonitor/
├── BigDataMonitorApp.kt
├── MainActivity.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── WorkerModule.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── entity/
│   │   │   ├── AppEntity.kt
│   │   │   ├── ClipboardEventEntity.kt
│   │   │   ├── ClipboardAccessEventEntity.kt
│   │   │   ├── NotificationEventEntity.kt
│   │   │   ├── AppUsageEventEntity.kt
│   │   │   ├── PermissionSnapshotEntity.kt
│   │   │   ├── NetworkEventEntity.kt
│   │   │   ├── SensitiveEventEntity.kt
│   │   │   ├── CorrelationResultEntity.kt
│   │   │   └── AppConfigEntity.kt
│   │   ├── dao/
│   │   │   ├── AppDao.kt
│   │   │   ├── ClipboardEventDao.kt
│   │   │   ├── ClipboardAccessEventDao.kt
│   │   │   ├── NotificationEventDao.kt
│   │   │   ├── AppUsageEventDao.kt
│   │   │   ├── PermissionSnapshotDao.kt
│   │   │   ├── NetworkEventDao.kt
│   │   │   ├── SensitiveEventDao.kt
│   │   │   ├── CorrelationResultDao.kt
│   │   │   └── AppConfigDao.kt
│   │   └── Converters.kt
│   ├── repository/
│   │   ├── ClipboardRepository.kt
│   │   ├── NotificationRepository.kt
│   │   ├── UsageRepository.kt
│   │   ├── PermissionRepository.kt
│   │   ├── NetworkRepository.kt
│   │   ├── SensitiveEventRepository.kt
│   │   └── CorrelationRepository.kt
│   └── datastore/
│       └── SettingsDataStore.kt
├── domain/
│   ├── model/
│   │   ├── EventType.kt
│   │   ├── RiskLevel.kt
│   │   ├── SensitivePermission.kt
│   │   ├── CorrelationResult.kt
│   │   └── TrackerDomains.kt
│   ├── CorrelationEngine.kt
│   └── RiskScorer.kt
├── service/
│   ├── PrivacyMonitorService.kt
│   ├── PrivacyAccessibilityService.kt
│   ├── NotificationMonitorService.kt
│   └── NetworkMonitorService.kt
├── worker/
│   ├── UsageStatsWorker.kt
│   ├── PermissionAuditWorker.kt
│   └── DataCleanupWorker.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Screen.kt
│   ├── components/
│   │   ├── EmptyState.kt
│   │   ├── LoadingState.kt
│   │   ├── EventItem.kt
│   │   ├── RiskBadge.kt
│   │   ├── ServiceStatusCard.kt
│   │   └── ConfidenceBar.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── timeline/
│   │   ├── TimelineScreen.kt
│   │   └── TimelineViewModel.kt
│   ├── applist/
│   │   ├── AppListScreen.kt
│   │   ├── AppListViewModel.kt
│   │   ├── AppDetailScreen.kt
│   │   └── AppDetailViewModel.kt
│   ├── correlation/
│   │   ├── CorrelationScreen.kt
│   │   └── CorrelationViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── onboarding/
│       └── OnboardingScreen.kt
├── util/
│   ├── PermissionUtil.kt
│   ├── HashUtil.kt
│   ├── ExportManager.kt
│   ├── AppInfoUtil.kt
│   └── NotificationHelper.kt
└── receiver/
    └── PackageChangeReceiver.kt
```

资源文件：
```
app/src/main/res/
├── xml/
│   ├── accessibility_service_config.xml
│   ├── backup_rules.xml
│   ├── data_extraction_rules.xml
│   └── file_paths.xml
├── values/
│   ├── strings.xml
│   ├── colors.xml
│   └── themes.xml
├── values-night/
│   └── themes.xml
├── drawable/
│   └── ic_launcher_foreground.xml
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
└── mipmap-mdpi/
    └── ic_launcher.webp (可占位，但不影响编译)
```

---

## 八、关键实现注意事项

### 8.1 NotificationListenerService 与 Hilt

`NotificationListenerService` 不能直接使用 `@AndroidEntryPoint`。使用 `EntryPointAccessors` 手动获取依赖：

```kotlin
class NotificationMonitorService : NotificationListenerService() {

    private val repository: NotificationRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationRepositoryEntryPoint::class.java
        ).notificationRepository()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationRepositoryEntryPoint {
        fun notificationRepository(): NotificationRepository
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        // ... 处理逻辑
    }
}
```

### 8.2 AccessibilityService 也用 EntryPoint

同理，`AccessibilityService` 也使用 `EntryPointAccessors` 获取依赖。

### 8.3 UsageStatsManager 权限检查

```kotlin
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
```

### 8.4 剪贴板 Toast 检测

Android 12 (API 31) 引入了剪贴板访问提示 Toast，文本类似：
- 中文："XXX 已从剪贴板粘贴"
- 英文："XXX pasted from your clipboard"

无障碍服务可通过 `TYPE_VIEW_TEXT_CHANGED` 或 `TYPE_NOTIFICATION_STATE_CHANGED` 事件捕获：
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
        val text = event.text?.joinToString(" ") ?: ""
        val sourcePackage = rootInActiveWindow?.packageName?.toString() ?: ""
        if (text.contains("粘贴") || text.contains("pasted") || text.contains("剪贴板")) {
            // 记录 ClipAccessEvent
        }
    }
}
```

### 8.5 前台服务通知

```kotlin
// Android 14 要求声明 foregroundServiceType
// 在 startForeground 时指定类型
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    startForeground(
        NOTIFICATION_ID,
        buildNotification(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    )
} else {
    startForeground(NOTIFICATION_ID, buildNotification())
}
```

### 8.6 DataStore 配置

```kotlin
// SettingsDataStore.kt
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val DARK_MODE = stringPreferencesKey("dark_mode") // "system" / "on" / "off"
    val CLIPBOARD_MONITOR_ENABLED = booleanPreferencesKey("clipboard_monitor_enabled")
    val NOTIFICATION_MONITOR_ENABLED = booleanPreferencesKey("notification_monitor_enabled")
    val USAGE_STATS_ENABLED = booleanPreferencesKey("usage_stats_enabled")
    val PERMISSION_AUDIT_ENABLED = booleanPreferencesKey("permission_audit_enabled")
    val NETWORK_MONITOR_ENABLED = booleanPreferencesKey("network_monitor_enabled")
    val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
    val NOTIFICATION_STORAGE_MODE = stringPreferencesKey("notification_storage_mode") // "summary" / "full"
    val CLIPBOARD_STORAGE_MODE = stringPreferencesKey("clipboard_storage_mode")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}
```

### 8.7 关键词匹配存储

`NotificationEventEntity.matchedTopicIds` 用 CSV 格式存储（如 "1,3,5"），读取时逗号分割转为 Long 列表。或者使用 Room TypeConverter。

### 8.8 导出功能

```kotlin
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // 各 DAO 注入
) {
    suspend fun exportJson(start: Long, end: Long): Uri {
        val data = ExportData(...)
        val json = Json { prettyPrint = true }.encodeToString(data)
        val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun exportCsv(start: Long, end: Long): Uri {
        // CSV 格式：timestamp, type, app, title, text, matchedTopics
    }
}
```

---

## 九、测试与编译验证

### 9.1 编译验证
确保项目可以在 Android Studio 中打开并成功编译：
- 所有 import 语句完整
- 所有抽象方法已实现
- 所有 Hilt 模块正确配置
- Room 编译器注解处理器配置正确（kapt）
- 没有未解析的引用

### 9.2 Gradle Wrapper
项目包含 `gradle/wrapper/gradle-wrapper.properties`，但不需要包含 `gradlew` 脚本（用户在 Android Studio 中打开时会自动下载）。properties 文件指定 Gradle 8.5 版本。

### 9.3 不需要编写的文件
- 不需要写单元测试
- 不需要写 UI 测试
- 不需要写 README（除非设计方案要求）

---

## 十、交付清单

请按以下清单确认所有文件已创建：

### Gradle 工程
- [ ] `settings.gradle.kts`
- [ ] `build.gradle.kts` (根模块)
- [ ] `gradle.properties`
- [ ] `gradle/wrapper/gradle-wrapper.properties`
- [ ] `app/build.gradle.kts`
- [ ] `app/proguard-rules.pro`

### Manifest 与资源
- [ ] `app/src/main/AndroidManifest.xml`
- [ ] `res/xml/accessibility_service_config.xml`
- [ ] `res/xml/file_paths.xml`
- [ ] `res/xml/backup_rules.xml`
- [ ] `res/xml/data_extraction_rules.xml`
- [ ] `res/values/strings.xml`
- [ ] `res/values/colors.xml`
- [ ] `res/values/themes.xml`
- [ ] `res/values-night/themes.xml`
- [ ] `res/drawable/ic_launcher_foreground.xml`
- [ ] `res/mipmap-anydpi-v26/ic_launcher.xml`
- [ ] `res/mipmap-anydpi-v26/ic_launcher_round.xml`

### Kotlin 代码
- [ ] `BigDataMonitorApp.kt`
- [ ] `MainActivity.kt`
- [ ] `di/` (3 个 Module)
- [ ] `data/db/` (1 Database + 10 Entity + 10 DAO + 1 Converters)
- [ ] `data/repository/` (7 Repository)
- [ ] `data/datastore/SettingsDataStore.kt`
- [ ] `domain/model/` (5 模型类)
- [ ] `domain/CorrelationEngine.kt`
- [ ] `domain/RiskScorer.kt`
- [ ] `service/` (4 Service)
- [ ] `worker/` (3 Worker)
- [ ] `ui/theme/` (3 文件)
- [ ] `ui/navigation/` (2 文件)
- [ ] `ui/components/` (6 通用组件)
- [ ] `ui/dashboard/` (2 文件)
- [ ] `ui/timeline/` (2 文件)
- [ ] `ui/applist/` (4 文件)
- [ ] `ui/correlation/` (2 文件)
- [ ] `ui/settings/` (2 文件)
- [ ] `ui/onboarding/` (1 文件)
- [ ] `util/` (5 工具类)
- [ ] `receiver/PackageChangeReceiver.kt`

**预计总文件数：约 65-70 个文件**

---

## 十一、特别提醒

1. **这是公益应用**，代码风格应体现专业性和可维护性，而非商业化堆砌。
2. **诚实是本应用的核心价值**。UI 中的"技术限制说明"、关联结果的"推断非因果"提示，都是产品的灵魂，不可省略或弱化。
3. **不申请 INTERNET 权限是铁律**。即使 VpnService 网络监控模块需要解析网络数据包，那也是通过 VPN Tunnel 在本地处理的，不涉及联网请求。
4. **主题色跟随系统是用户的明确需求**，必须作为设置页面的第一个开关，并默认开启。
5. 每个文件必须完整实现，不得有省略。如果某个方法过长，用完整代码而非注释占位。
6. 数据库 Entity 中的 `matchedTopicIds` 等列表型字段，优先使用 Room TypeConverter 转换。
7. 所有 Service 类的 `onDestroy` / `onUnbind` 要正确清理资源。
8. WorkManager 的周期性任务最小间隔为 15 分钟，不要设置更短。
9. 写完所有文件后，重新检查一遍 import 是否完整、类名是否一致、包名是否匹配。
10. **先阅读 `DesignDocument.md` 再写代码。**

---

## 结束

请基于以上提示词和 `DesignDocument.md` 设计方案，创建完整的 Android 项目。所有代码文件写入项目文件夹下。

> AI生成