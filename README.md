# 跨主体数据鉴权系统 - 端侧数据采集应用 (IMSI-IMEI)
## 📖项目介绍
本项目是跨主体数据鉴权系统的移动终端侧组件，基于 Android 平台开发。核心是采集设备硬件、软件及SIM卡凭证信息，生成JSON数据存储至本地并上传至服务器，以支持生成唯一的身份凭证和后续的鉴权服务。

## 🚀功能特性
当前版本已实现静态数据采集、本地导出以及同步上传至服务器功能。

### 1. 数据采集
应用通过 ga.mdm.DeviceInfoManager 类收集以下三类核心凭证数据：  
硬件信息：设备名称、型号名称、硬件序列号 (SN码) 。设备唯一标识信息 (IMEI)、CPU、内存、基带版本。  
软件信息：操作系统名称、操作系统版本、Android ID、内核版本信息。  
网络身份信息(SIM卡属性)：双卡手机号码、IMSI (国际移动用户识别码)、ICCID (集成电路卡识别码)。

### 2. 数据处理与导出
自动采集：点击应用启动后，自动调用采集方法。  
格式化存储：将采集的数据导出为 JSON 格式文件，并保存在应用外部存储目录中并传至服务器端数据库。

### 3. 用户界面 (UI)
包含四个主要交互界面 ：  
MainActivity：主界面，显示本机号码及功能导航。  
HardwareInfoActivity：展示详细的硬件属性。  
SoftwareInfoActivity：展示操作系统及软件环境信息。  
SimInfoActivity：展示SIM卡及网络相关信息。

## 🏗️系统架构与鉴权流程
本应用作为智能终端侧的“采集插件”，与后端的“主体认证服务器”协同工作。  
· 数据采集：终端采集设备硬件、软件及网络凭证数据，以json格式存储至本地。  
· 安全传输：通过 TLS/SSL 加密通道将数据上传至服务器。  
· 凭证生成：服务器通过哈希算法提取特征，生成唯一的码号关联主体身份凭证，并存入证据链数据库。  
· 认证与授权：用户认证通过后，服务器发放唯一授权 Token。用户访问第三方服务时携带该 Token，第三方服务向认证服务器验证 Token 合法性。

## 🛠️开发环境及版本配置
### 开发环境
IDE: Android Studio (Koala 2024.1.2)  
JDK/JVM：JDK 11  
Java Compatibility：Java 8 (1.8)  
Gradle Wrapper：7.0.2  
Android Gradle Plugin：7.0.2  
Kotlin Version：1.7.10

### SDK版本配置
Compile SDK: API 30 (Android 11)  
Target SDK: API 30 (Android 11)  
Min SDK: API 26 (Android 8.0)

## ⚙️权限说明
### 隐私权限适配
本项目涉及设备指纹（IMEI, SN等）的高精度采集。受 Android 10 (API 29+) 隐私沙盒机制限制，非系统应用无法直接获取此类信息。本项目采用以下技术方案规避限制：
1.  **系统级集成**：依赖终端公司提供的定制 SDK (`.jar`)，并通过 **PAC 刷机** 将应用提升为系统级权限。  
2.  **核心实现**：通过 `ga.mdm.DeviceInfoManager` 类封装系统接口调用逻辑。
### 运行前置指令
由于项目需通过反射或私有接口访问系统底层数据，每次调试或运行前，请连接设备并执行以下ADB命令，以允许调用隐藏API：
```bash
# 解除隐藏 API 调用限制
adb shell settings put global hidden_api_policy 1
```
## 📂项目结构
src/  
├── main/  
│   ├── java/com/imsi/imei/activities/  
│   │   ├── MainActivity.java              // 主入口   
│   │   ├── HardwareInfoActivity.java      // 硬件信息页   
│   │   ├── SoftwareInfoActivity.java      // 软件信息页   
│   │   └── SimInfoActivity.java           // SIM信息页   
│   └── java/com/imsi/imei/res/  
│       └── layout/                        // UI 布局文件  
└── AndroidManifest.xml

## 📅 后续计划
项目后续的重点迭代将围绕以下需求展开：
### 1. 周期性数据采集和更新
实现定时任务机制、数据对比逻辑及文件管理功能。  
· 保留已有触发采集功能  
· 增加每24h一次的周期性采集  
· 认证时，用户访问第三方服务的操作会触发数据采集模块采集当前信息，不存至本地，直接上传到服务器与该用户的凭证进行比对    
**为采集存储信息过程，若为首次采集，则直接存入本地；若非首次采集，则对比此次采集的新数据和本地存储数据（上次采集得到）的内容，若数据发生变化，则保存新文件并删除旧文件；若数据无变化，则不做文件更新操作；③为认证辅助过程。**
### 2. 端侧数据上传
本地更新JSON文件时，同步上传至服务器，更新服务器数据库中的用户信息。
