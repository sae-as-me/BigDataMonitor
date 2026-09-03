
# BigDataMonitor — 个人隐私数据监控器

## 项目简介

BigDataMonitor 是一款 Android 端隐私行为感知工具，帮助用户理解"我的哪些个人隐私信息、在什么时间、被哪些 App 获取或用于了个性化推荐"。

应用采用 **信号采集 + 关联分析 + 可视化呈现** 的策略：利用 Android 合法 API（无障碍服务、通知监听、剪贴板监听、使用统计、权限审计）收集多条隐私相关的行为信号，让用户标注敏感事件后，系统在时间线上高亮该事件前后各 App 的异常行为，以时间线、应用详情、风险评分等方式，让隐形的隐私数据流变为可见。

### 产品属性

- **公益属性**：无广告、无内购、无数据上传
- **最小权限**：仅申请实现功能所必需的系统权限，**不申请 INTERNET 权限**
- **开源透明**：所有监控逻辑对用户可见，监控数据仅存于本地

### 技术边界（诚实声明）

| 能做到 | 做不到 |
|--------|--------|
| 监听剪贴板内容变化并记录时间戳和内容摘要 | 直接监听其他 App 的麦克风采集行为 |
| 通过无障碍服务捕获 Android 12+ 的"App 已从剪贴板粘贴"系统提示，识别读取剪贴板的 App | 截获 App 之间的数据共享（如 SDK 跨 App 追踪） |
| 通过 NotificationListenerService 捕获所有通知内容，记录来源 App 和时间 | 精确判断某个推荐内容是否由某次线下对话触发（关联分析是统计推断，非因果证明） |
| 通过 UsageStatsManager 追踪各 App 前后台切换时间线 | 获取其他 App 内部的传感器调用记录 |
| 扫描所有已安装 App 的权限声明清单，标识持有敏感权限的 App | 读取其他 App 的私有数据 |
| 通过 VpnService（可选）分析应用网络连接目标域名，识别追踪域名 | 解密 HTTPS 流量内容 |

---

## 技术栈

| 类别 | 技术选型 | 版本 |
|------|---------|------|
| 语言 | Kotlin | 1.9.22 |
| UI 框架 | Jetpack Compose | BOM 2024.02.00 |
| 设计体系 | Material 3 (Material You) | 随 Compose BOM |
| 导航 | Navigation Compose | 2.7.7 |
| 依赖注入 | Hilt (Dagger) | 2.50 |
| 本地数据库 | Room | 2.6.1 |
| 偏好存储 | DataStore Preferences | 1.0.0 |
| 后台任务 | WorkManager | 2.9.0 |
| 序列化 | kotlinx.serialization | 1.6.2 |
| 协程 | kotlinx.coroutines | 1.7.3 |
| 构建工具 | Gradle (Kotlin DSL) | 8.5 |
| Android Gradle Plugin | AGP | 8.2.2 |
| 编译器扩展 | Compose Compiler | 1.5.8 |

### 最低兼容

- **minSdk**: 29 (Android 10)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34
- **JDK**: 17

---

## 环境依赖

### 必需环境

1. **JDK 17**（推荐 Eclipse Temurin / BellSoft Liberica / Oracle JDK 17）
   - 验证：`java -version` 输出 `17.x.x`
   - 环境变量：`JAVA_HOME` 指向 JDK 安装目录

2. **Android SDK**
   - 需安装以下组件：
     - `platform-tools`（最新版）
     - `platforms;android-34`
     - `build-tools;34.0.0`
   - 环境变量：`ANDROID_HOME`（或 `ANDROID_SDK_ROOT`）指向 SDK 根目录

3. **Gradle 8.5**
   - 项目自带 Gradle Wrapper（`gradlew` / `gradlew.bat`），首次执行时会自动下载
   - 如自动下载失败（网络问题），可手动下载 `gradle-8.5-bin.zip` 并放置到本地，修改 `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl` 指向本地文件

### 可选环境

- **Android Studio**（Hedgehog 2023.1.1 或更高版本）：用于 UI 预览、调试、签名打包
- **命令行**：仅需 JDK 17 + Android SDK 即可编译

---

## 项目结构

```
BigDataMonitor/
├── build.gradle.kts                    # 根构建脚本（声明插件版本）
├── settings.gradle.kts                 # 项目设置（模块声明、仓库配置）
├── gradle.properties                   # Gradle 全局属性
├── gradlew / gradlew.bat               # Gradle Wrapper 脚本
├── gradle/wrapper/                      # Gradle Wrapper 文件
├── local.properties                    # SDK 路径配置（自动生成，不入版本库）
│
└── app/
    ├── build.gradle.kts                # 模块构建脚本（依赖声明、编译配置）
    ├── proguard-rules.pro              # ProGuard 规则
    └── src/main/
        ├── AndroidManifest.xml          # 清单文件（权限、Service、Receiver 声明）
        ├── java/com/bigdatamonitor/
        │   ├── BigDataMonitorApp.kt     # Application 入口（Hilt）
        │   ├── MainActivity.kt          # 唯一 Activity（Compose 宿主）
        │   │
        │   ├── data/                     # 数据层
        │   │   ├── datastore/            # DataStore 偏好存储
        │   │   ├── db/                   # Room 数据库
        │   │   │   ├── AppDatabase.kt    # 数据库定义（10 个 Entity + 10 个 DAO）
        │   │   │   ├── dao/              # DAO 接口
        │   │   │   └── entity/           # 数据实体
        │   │   └── repository/          # Repository 模式（7 个仓库）
        │   │
        │   ├── di/                       # 依赖注入
        │   │   ├── DatabaseModule.kt     # 数据库 & DAO 提供
        │   │   ├── RepositoryModule.kt   # Repository 提供
        │   │   └── WorkerModule.kt       # Worker 注入
        │   │
        │   ├── domain/                   # 领域层
        │   │   ├── CorrelationEngine.kt  # 关联分析引擎
        │   │   ├── RiskScorer.kt         # 风险评分器
        │   │   └── model/                # 领域模型
        │   │       ├── CorrelationResult.kt
        │   │       ├── EventType.kt
        │   │       ├── RiskLevel.kt
        │   │       ├── SensitivePermission.kt
        │   │       └── TrackerDomains.kt
        │   │
        │   ├── service/                  # 系统服务
        │   │   ├── PrivacyMonitorService.kt        # 前台监控服务
        │   │   ├── PrivacyAccessibilityService.kt   # 无障碍服务（剪贴板 + 前后台）
        │   │   ├── NotificationMonitorService.kt    # 通知监听服务
        │   │   └── NetworkMonitorService.kt        # VPN 网络监控服务（可选）
        │   │
        │   ├── receiver/
        │   │   └── PackageChangeReceiver.kt        # 应用安装/卸载监听
        │   │
        │   ├── worker/                    # 后台定时任务
        │   │   ├── UsageStatsWorker.kt    # 使用统计采集（15 分钟）
        │   │   ├── PermissionAuditWorker.kt # 权限审计（24 小时）
        │   │   ├── DataCleanupWorker.kt   # 数据清理（24 小时）
        │   │   └── WorkScheduler.kt      # 任务调度器
        │   │
        │   ├── ui/                        # UI 层
        │   │   ├── MainViewModel.kt       # 主 ViewModel（引导状态）
        │   │   ├── navigation/             # 导航
        │   │   ├── theme/                  # 主题（Color / Theme / Type）
        │   │   ├── components/             # 通用组件
        │   │   ├── dashboard/              # 仪表盘页
        │   │   ├── timeline/               # 时间线页
        │   │   ├── applist/                # 应用列表 + 详情页
        │   │   ├── correlation/            # 关联分析页
        │   │   ├── settings/              # 设置页
        │   │   └── onboarding/             # 首次启动引导
        │   │
        │   └── util/                      # 工具类
        │       ├── HashUtil.kt            # 内容哈希
        │       ├── PermissionUtil.kt      # 权限检查
        │       ├── AppInfoUtil.kt         # 应用信息
        │       ├── NotificationHelper.kt  # 通知工具
        │       └── ExportManager.kt       # 数据导出（JSON/CSV）
        │
        └── res/                           # 资源文件
            ├── values/                     # 字符串、颜色、主题
            ├── values-night/               # 深色主题
            ├── drawable/                   # 矢量图标
            ├── mipmap-anydpi-v26/         # 自适应图标
            └── xml/                        # 配置文件
```

---

## 编译方法

### 方法一：命令行编译（推荐）

#### 1. 配置环境变量

```powershell
# Windows PowerShell（当前会话有效）
$env:JAVA_HOME = "<JDK 17 路径>"
$env:ANDROID_HOME = "<Android SDK 路径>"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

```bash
# Linux/macOS
export JAVA_HOME=<JDK 17 路径>
export ANDROID_HOME=<Android SDK 路径>
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH
```

#### 2. 创建 local.properties

在项目根目录创建 `local.properties` 文件：

```properties
sdk.dir=<Android SDK 绝对路径>
# Windows 示例：sdk.dir=D\:\\Android\\Sdk
# Linux 示例：sdk.dir=/home/user/Android/Sdk
```

> 此文件也可不手动创建，首次运行 `gradlew` 时 Gradle 会尝试自动检测 SDK 路径并生成。

#### 3. 执行编译

```powershell
# 进入项目目录
cd BigDataMonitor

# 编译 Debug APK
.\gradlew.bat assembleDebug

# 编译 Release APK（需配置签名）
.\gradlew.bat assembleRelease

# 清理后重新编译
.\gradlew.bat clean assembleDebug
```

```bash
# Linux/macOS
./gradlew assembleDebug
```

#### 4. 获取 APK

编译成功后，APK 文件位于：

```
app/build/outputs/apk/debug/app-debug.apk        # Debug 版本
app/build/outputs/apk/release/app-release.apk   # Release 版本
```

### Debug 版本与 Release 版本的区别

本项目在 `app/build.gradle.kts` 中定义了两个构建类型。两者的核心差异如下：

| 对比项 | Debug 版本 | Release 版本 |
|--------|-----------|--------------|
| **构建命令** | `gradlew assembleDebug` | `gradlew assembleRelease` |
| **APK 路径** | `app/build/outputs/apk/debug/app-debug.apk` | `app/build/outputs/apk/release/app-release.apk` |
| **应用签名** | 自动使用 Android SDK 内置的 Debug 签名（`~/.android/debug.keystore`），无需手动配置 | 需要自行配置签名密钥（keystore），未配置则编译产物为未签名 APK，无法直接安装 |
| **代码混淆** | 关闭（`isMinifyEnabled = false`） | 当前项目同样关闭，如需启用可设为 `true` 并配置 ProGuard 规则 |
| **可调试性** | 允许 ADB 调试、日志输出、断点调试 | 默认不可调试，日志输出更精简 |
| **APK 体积** | 较大（约 54 MB，含调试信息和未压缩资源） | 启用混淆和资源压缩后体积会显著减小 |
| **性能** | 略低于 Release（JIT 优化不充分） | 更高（启用混淆和 R8 优化后执行效率更优） |
| **适用场景** | 开发调试、功能验证、自用安装 | 正式发布、分发给别人、应用商店上架 |

#### Release 版本签名配置

Release 版本必须配置签名才能生成可安装的 APK。在 `app/build.gradle.kts` 中添加签名配置：

```kotlin
android {
    // ... 其他配置 ...

    signingConfigs {
        create("release") {
            // 密钥库文件路径（需提前生成）
            storeFile = file("release.keystore")
            // 密钥库密码
            storePassword = "your_store_password"
            // 密钥别名
            keyAlias = "release_key"
            // 密钥密码
            keyPassword = "your_key_password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // 如需启用混淆改为 true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

生成签名密钥库（keystore）：

```powershell
# 使用 JDK 自带的 keytool 生成密钥库
keytool -genkey -v -keystore release.keystore -alias release_key -keyalg RSA -keysize 2048 -validity 10000
# 按提示输入密钥库密码、密钥密码、姓名、组织等信息
```

> 安全提示：不要将 keystore 文件和密码提交到版本库。建议在 `local.properties` 或环境变量中管理敏感信息，通过 `build.gradle.kts` 读取。

#### 如何选择版本

- **日常自用、测试功能**：直接使用 Debug 版本，编译命令 `gradlew assembleDebug`，产物无需签名即可安装
- **分享给他人、长期使用**：建议编译 Release 版本并签名，启用混淆以减小体积和提升性能

### 方法二：Android Studio 编译

1. 打开 Android Studio
2. 选择 `File` → `Open` → 选中项目根目录 `BigDataMonitor/`
3. 等待 Gradle Sync 完成（首次会自动下载依赖，可能需要 5-15 分钟）
4. 点击 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
5. 编译完成后点击通知栏的 `locate` 链接打开 APK 所在目录

### 国内网络加速（可选）

如遇依赖下载缓慢或超时，可在 `settings.gradle.kts` 中添加国内镜像：

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}
```

或在项目根目录创建 `init.d/init.gradle`：

```groovy
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
```

---

## 使用方法

### 安装

#### Debug 版本安装

Debug 版本使用自动签名，编译完成后可直接安装：

```powershell
# 通过 ADB 安装到已连接的设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

或直接将 `app-debug.apk` 传输到手机，点击安装（需开启"允许安装未知来源应用"）。

#### Release 版本安装

Release 版本需先完成签名配置（见上方"Release 版本签名配置"章节），编译签名后的 APK：

```powershell
# 编译 Release 版本
.\gradlew.bat assembleRelease

# 通过 ADB 安装
adb install app/build/outputs/apk/release/app-release.apk
```

> 如果未配置签名，编译出的 `app-release-unsigned.apk` 无法直接安装，需先签名：
> ```powershell
> # 对未签名 APK 进行签名
> apksigner sign --ks release.keystore --ks-key-alias release_key app-release-unsigned.apk
> # 对齐优化
> zipalign -v 4 app-release-unsigned.apk app-release.apk
> ```

### 首次启动引导

1. 打开 BigDataMonitor
2. 欢迎页：阅读应用介绍
3. 权限授权页：依次开启以下系统权限
   - **无障碍服务**：检测剪贴板访问行为和应用前后台切换
   - **通知监听**：捕获推送通知内容
   - **使用统计**：追踪应用使用时间线
4. 技术限制说明页：了解应用能做到和做不到的事情，勾选"我已了解"
5. 点击"开始使用"进入主界面

### 功能页面

#### 仪表盘（首页）
- 今日隐私事件统计（剪贴板被读取次数、疑似关联通知数、高风险应用数）
- 隐私风险排行榜
- 今日事件时间线缩略
- 监控服务运行状态

#### 时间线
- 按时间倒序展示所有监控事件
- 支持按事件类型筛选：全部、剪贴板、通知、使用、网络、关联
- 每条事件显示时间、类型图标、来源 App、内容摘要

#### 应用列表
- 列出所有已安装的非系统应用
- 显示每个应用的风险评分和风险等级
- 点击应用进入详情页，查看：
  - 基本信息（包名、安装来源）
  - 敏感权限清单（按敏感度分级）
  - 近期事件记录
  - 网络连接记录（含追踪域名标识）

#### 关联分析
- **标注敏感事件**：手动记录你认为敏感的事件（如"14:00 与朋友谈论爬山"）
- 系统在时间线上高亮该事件前后各 App 的行为
- 展示关联结果及可信度
- ⚠ 关联为推断分析，非因果证明

#### 设置
- **外观**：主题色跟随系统开关（Material You 动态取色，Android 12+）、深色模式
- **监控服务**：剪贴板监控、通知监控、使用统计追踪、权限审计、网络监控（VPN）开关
- **数据存储**：数据保留天数、通知内容存储级别、剪贴板内容存储级别
- **数据导出**：导出为 JSON / CSV
- **关于**：版本号、隐私声明、技术限制说明

### 需要开启的系统权限

| 权限 | 用途 | 开启方式 |
|------|------|---------|
| 无障碍服务 | 检测剪贴板访问 Toast、应用前后台切换 | 设置 → 无障碍 → BigDataMonitor |
| 通知监听 | 捕获所有推送通知内容 | 设置 → 通知访问 → BigDataMonitor |
| 使用统计 | 追踪各 App 前后台切换时间线 | 设置 → 应用使用权限 → BigDataMonitor |
| VPN（可选） | 分析应用网络连接目标域名 | 设置页内开关，开启后系统会弹 VPN 授权对话框 |

### 数据导出

在设置页选择"导出为 JSON"或"导出为 CSV"，系统将本地数据库中的所有监控数据导出为文件，可通过系统分享面板保存或发送。

---

## 核心技术实现

### 监控信号采集

| 信号源 | Android API | 采集内容 |
|--------|-------------|---------|
| 无障碍服务 | `AccessibilityService` | Android 12+ 剪贴板访问 Toast、窗口状态变化（前后台切换） |
| 通知监听 | `NotificationListenerService` | 所有 App 的推送通知（标题、文本、来源包名、时间戳） |
| 使用统计 | `UsageStatsManager` | 各 App 前后台切换事件和时间 |
| 权限审计 | `PackageManager.getPackageInfo` + `AppOpsManager` | 已安装 App 的权限声明和敏感操作记录 |
| 剪贴板 | `ClipboardManager` | 剪贴板内容变化时间戳和内容哈希 |
| 网络监控 | `VpnService`（可选） | 应用网络连接的目标域名和协议，识别追踪域名 |

### 关联分析引擎

用户标注一个敏感事件后（如"14:00 谈论爬山"），系统在时间线上搜索该事件前后一定时间窗口内的异常行为：

- 该时间窗口内是否有 App 读取了剪贴板
- 该时间窗口内是否收到了个性化推荐通知
- 该时间窗口内是否有 App 频繁切到前台
- 该时间窗口内是否有 App 连接了已知追踪域名

根据多个信号的叠加计算关联可信度（0-100%），但结果为推断分析，不代表因果关系。

### 风险评分

每个已安装应用的风险评分（0-100）基于以下维度综合计算：

- 持有的敏感权限数量和敏感度等级
- 读取剪贴板的次数
- 通知推送频率
- 是否连接追踪域名
- 是否在敏感事件时间窗口内有异常行为

风险等级划分：
- 0：未知
- 1-29：低风险
- 30-59：中风险
- 60-84：高风险
- 85-100：极高风险

### 数据存储

所有数据存储在本地 Room 数据库，包含以下 10 张表：

| 表名 | 内容 |
|------|------|
| `apps` | 已安装应用信息（包名、名称、风险评分、是否系统应用） |
| `app_config` | 应用监控配置 |
| `app_usage_events` | 应用前后台切换事件 |
| `clipboard_events` | 剪贴板内容变化事件 |
| `clipboard_access_events` | 剪贴板被读取事件 |
| `notification_events` | 通知事件 |
| `network_events` | 网络连接事件 |
| `permission_snapshots` | 权限快照 |
| `sensitive_events` | 用户标注的敏感事件 |
| `correlation_results` | 关联分析结果 |

数据保留天数可在设置中调整，超过保留期的数据由 `DataCleanupWorker` 定时清理。

### 后台任务

| 任务 | 周期 | 说明 |
|------|------|------|
| 使用统计采集 | 15 分钟 | 通过 UsageStatsManager 采集 App 使用数据 |
| 权限审计 | 24 小时 | 扫描所有已安装 App 的权限变化 |
| 数据清理 | 24 小时 | 清理超过保留期的历史数据（需设备空闲） |

---

## 权限声明

本应用仅申请以下权限，**不申请 INTERNET 权限**：

| 权限 | 用途 |
|------|------|
| `POST_NOTIFICATIONS` | 发送前台服务通知（Android 13+） |
| `FOREGROUND_SERVICE` | 运行监控前台服务 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 声明前台服务用途（隐私监控） |
| `QUERY_ALL_PACKAGES` | 查询所有已安装应用信息（权限审计需要） |

以下系统权限通过系统设置开启，不在 Manifest 中声明：
- 无障碍服务（`BIND_ACCESSIBILITY_SERVICE`）
- 通知监听（`BIND_NOTIFICATION_LISTENER_SERVICE`）
- 使用统计（应用使用权限）
- VPN 服务（`BIND_VPN_SERVICE`，可选）

---

## 隐私声明

- **不联网**：本应用不申请 INTERNET 权限，不会向任何服务器发送数据
- **不上传**：所有监控数据仅存储在设备本地 Room 数据库
- **不收集**：不包含任何分析 SDK、崩溃上报、广告 SDK
- **用户掌控**：用户可随时查看、导出、删除所有监控数据

---

## 已知限制

1. **剪贴板读取检测**仅在 Android 12+ 有效（依赖系统弹出 Toast 提示）
2. **通知内容**仅能获取通知标题和文本，无法获取 App 内部推送逻辑
3. **网络监控**通过 VpnService 实现，会接管设备网络流量，可能影响连接速度
4. **关联分析**是统计推断而非因果证明，结果仅供参考
5. **使用统计**数据由系统记录，精确度取决于系统实现
6. **权限审计**只能查看权限声明和部分操作记录，无法监控运行时权限使用

---

## 构建配置

### Gradle 属性（`gradle.properties`）

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
kapt.use.worker.api=true
kapt.incremental.apt=true
```

### 编译选项

- Java 源码兼容性：17
- Kotlin JVM Target：17
- Compose 编译器扩展：1.5.8
- 代码混淆（Release）：默认关闭，可按需启用并配置 ProGuard 规则

---

## 版本信息

- **版本号**：1.0.0
- **versionCode**：1
- **applicationId**：com.bigdatamonitor
- **APK 大小**（Debug）：约 54 MB

---

## FAQ

### Q: 编译时报 "Could not load wrapper properties" 或下载 Gradle 超时？

A: 网络问题导致 Gradle Wrapper 无法下载。解决方案：
1. 手动下载 `gradle-8.5-bin.zip`
2. 修改 `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl` 为本地文件路径：
   ```properties
   distributionUrl=file\:/D:/path/to/gradle-8.5-bin.zip
   ```

### Q: 编译时报 "SDK location not found"？

A: 未配置 Android SDK 路径。在项目根目录创建 `local.properties`：
```properties
sdk.dir=D\:\\Android\\Sdk
```

### Q: KAPT 阶段耗时很长？

A: Hilt 和 Room 的注解处理器首次运行需要较长时间，后续增量编译会快很多。建议分配足够内存：
```properties
org.gradle.jvmargs=-Xmx4096m
```

### Q: 安装后无障碍服务/通知监听无法开启？

A: 这些权限需要在系统设置中手动开启，不能通过代码自动授予。请按照 App 内引导页的步骤操作。部分定制 ROM 可能限制了无障碍服务的使用。

### Q: 网络监控开启后无法上网？

A: VpnService 会接管设备网络流量。如果遇到问题，请在设置页关闭"网络监控"开关。该功能为可选项，不影响其他监控功能。

### Q: 应用占用存储空间过大？

A: 通知和事件数据会持续累积。可在设置中：
1. 减少"数据保留天数"（默认 30 天）
2. 选择"立即清理数据"
3. 降低"通知内容存储"级别为"仅摘要"

### Q: Release 版本编译后 APK 提示"未签名"无法安装？

A: Release 版本需要在 `app/build.gradle.kts` 中配置 `signingConfigs`，提供 keystore 文件路径和密码。详见"Release 版本签名配置"章节。Debug 版本使用自动签名，无需手动配置。

### Q: Debug 和 Release 版本可以同时安装在同一台手机上吗？

A: 不可以。两个版本使用相同的 `applicationId`（`com.bigdatamonitor`），系统只允许同一 applicationId 的一个版本存在。如需共存，可给 Debug 版本添加后缀：在 `build.gradle.kts` 的 `debug` 构建类型中设置 `applicationIdSuffix = ".debug"`，这样两个版本的包名不同，可以共存。

> AI生成