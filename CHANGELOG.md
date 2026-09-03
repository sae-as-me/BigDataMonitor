
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-09-03

### Added

- 隐私行为感知核心能力：无障碍服务、通知监听、剪贴板监听、使用统计、权限审计五大信号采集模块
- 时间线视图：按时间轴展示各 App 的隐私相关行为，支持敏感事件标注与前后行为高亮
- 应用详情页：展示单个 App 的权限声明、行为记录与隐私风险评分
- 风险评分体系：基于权限敏感度与行为频率的综合风险评分模型
- 可选 VPN 流量分析：识别应用网络连接目标域名中的追踪域名（默认关闭，不申请 INTERNET 权限）
- 本地优先存储：Room 数据库 + DataStore 偏好存储，所有监控数据仅存于本地设备
- Material You 动态主题，支持 Android 10（API 29）及以上版本

### Security

- 不申请 INTERNET 权限，监控数据零上传、零分享
- 最小权限原则：仅申请实现功能所必需的系统权限

> AI生成