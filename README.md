<div align="center">

<img src="./logo/mio-voice.png" width="120" alt="Mio Voice Logo">

# Mio Voice

一个简洁、可扩展的 Android TTS 客户端，用于管理音色、创建情绪预设，并通过语音服务生成自然音频。

[下载 APK](../../releases) · [功能介绍](#主要功能) · [开发计划](#开发计划) · [参与贡献](#参与贡献)

</div>

---

> [!WARNING]
> Mio Voice 目前处于 Alpha 开发阶段。
>
> 核心语音生成流程已经可以使用，但部分功能、界面和异常处理仍在持续完善中。使用前请自行保管好 API Key，并注意第三方服务可能产生的费用。

## 项目简介

Mio Voice 是一个面向 Android 平台的文本转语音应用。

很多 TTS 服务虽然提供了完整的 API，但在实际使用时，用户往往需要反复填写模型、音色、情绪和其他生成参数。

Mio Voice 希望将这些配置整理为更加直观的音色库和情绪预设，让语音生成不再只是一次性的 API 调用，而是一个可以长期使用和管理的完整应用。

项目目前主要围绕 MiniMax TTS 进行开发，同时也在为后续接入更多云端 TTS 服务预留扩展能力。

## 界面预览

<div align="center">

<img src="./docs/images/home.jpg" width="220" alt="Mio Voice 首页">
<img src="./docs/images/voice-library.jpg" width="220" alt="Mio Voice 音色库">
<img src="./docs/images/voice-list.jpg" width="220" alt="Mio Voice 语音库">

</div>

## 主要功能

### 文本转语音

- 输入文本并生成语音
- 选择音色和情绪预设
- 播放生成结果
- 支持长文本分段生成
- 显示生成进度和错误信息

### 音色库

- 添加和管理 TTS 音色
- 使用“父音色 → 子情绪预设”结构
- 为同一个基础音色创建不同表达风格
- 保存音色名称、Voice ID 和简介
- 统一管理不同场景下使用的语音配置

### 情绪预设

可以在同一个基础音色下创建多个不同的情绪或表达预设，例如：

- 开心
- 悲伤
- 生气
- 害怕
- 惊讶
- 中性
- 自定义表达风格

每个预设可以保存独立的情绪、语速、音量、音调和其他生成参数。

不同 TTS 服务对于情绪参数的支持可能存在差异，最终生成效果由对应服务决定。

### 生成记录

- 查看历史生成内容
- 查看完整原始文本
- 查看生成时间、音色和模型信息
- 播放历史生成音频
- 查看长文本生成后的音频分段

### 独立服务配置

应用将不同类型的服务配置分别管理：

- TTS 语音服务配置
- AI 文本分析服务配置

避免将多个服务地址、模型和密钥堆叠在同一个页面中。

### AI 文本分析

可选配兼容的 AI 服务，对输入文本进行分析，为长文本分段和语音表达提供辅助。

AI 分析服务与 TTS 服务相互独立。未配置 AI 服务时，仍然可以使用基础的文本转语音功能。

> AI 文本分析目前仍属于实验性功能，实际效果取决于所配置的模型和服务。

## 当前支持情况

| 服务 | 文本转语音 | 音色管理 | 情绪预设 | 当前状态 |
|---|---:|---:|---:|---|
| MiniMax | ✅ | ✅ | ✅ | 已支持 |
| 其他云端 TTS | ❌ | ❌ | ❌ | 规划中 |
| IndexTTS2 | ❌ | ❌ | ❌ | 长期评估 |
| GPT-SoVITS | ❌ | ❌ | ❌ | 长期评估 |

> 当前版本主要针对 MiniMax TTS 进行适配。
>
> “规划中”与“长期评估”不代表已经承诺支持，后续将根据接口形式、设备条件和实际需求逐步决定。

## 下载安装

前往项目的 [Releases](../../releases) 页面下载最新版本 APK。

当前测试版本：

```text
Mio Voice v0.1.0 Alpha
```

下载对应的 APK 文件后，即可在 Android 设备上安装。

你也可以同时下载 `.sha256` 文件，用于验证 APK 文件是否完整。校验示例：

```bash
# Linux / macOS
sha256sum -c Mio-Voice-v0.1.0-alpha.apk.sha256

# Windows (PowerShell)
Get-FileHash Mio-Voice-v0.1.0-alpha.apk -Algorithm SHA256
```

### 系统要求

- Android 最低版本：Android 8.0（API 26）
- 推荐 Android 版本：Android 13 及以上（应用以 API 35 / Android 15 为目标平台编译）
- 需要网络连接
- 需要用户自行准备受支持 TTS 服务的 API 地址和密钥

由于当前版本仍处于早期测试阶段，不建议将其用于关键或生产用途。

## 数据与隐私

Mio Voice 本身不提供语音模型或 AI 模型服务。

生成语音时，用户输入的文本会被发送至自行配置的第三方 TTS 服务。启用 AI 文本分析时，相关文本也可能被发送至用户配置的 AI 服务。

请注意：

- 项目仓库不包含任何真实 API Key
- 用户需要自行申请和保管第三方服务密钥
- 请勿将 API Key 分享给其他人
- 文本和音频的处理方式取决于对应第三方服务
- 使用前请阅读对应服务的隐私政策和使用条款
- 第三方 API 产生的费用由用户自行承担

关于 API Key 的本地存储：

> 应用使用 Android Keystore 生成的密钥对 API Key 进行 AES-GCM 加密，仅将加密后的密文保存在用户本地设备上（基于 DataStore）。密钥本身由系统 Keystore 管理，不会随明文一同存储，也不会上传到任何服务器。

## 从源码构建

### 开发环境

项目主要使用以下技术：

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Media3 (ExoPlayer)
- OkHttp
- Kotlin Coroutines
- Android Gradle Plugin
- Gradle Wrapper

推荐开发环境：

- Android Studio：Ladybug（2024.2.1）或更高版本（需支持 AGP 8.7）
- JDK：JDK 17
- Android SDK：compileSdk 35（Android SDK Platform 35 + Build-Tools 35 及以上）
- Kotlin：2.0.21（以项目 Gradle 配置为准）
- Gradle：8.9（由 Gradle Wrapper 提供，无需单独安装）

### 克隆项目

```bash
git clone https://github.com/miofling/mio-voice.git
cd mio-voice
```

### 构建 Debug APK

Windows：

```bash
gradlew.bat assembleDebug
```

macOS / Linux：

```bash
./gradlew assembleDebug
```

构建完成后，APK 通常位于：

```text
app/build/outputs/apk/debug/
```

也可以直接通过 Android Studio 构建：

```text
Build → Build App Bundle(s) / APK(s) → Build APK(s)
```

## Release 签名

为了保护开发者的签名信息，项目仓库不会包含：

- 正式 Keystore 文件
- Keystore 密码
- Key Alias
- Key Password
- 真实 API Key
- 本地环境配置

开发者可以自行创建 Keystore，并在本地配置 Release 签名。构建脚本会在项目根目录读取 `keystore.properties`，需要包含以下字段：

```properties
storeFile=相对项目根目录的 keystore 路径
storePassword=你的 keystore 密码
keyAlias=你的 key alias
keyPassword=你的 key 密码
```

> 若 `keystore.properties` 缺失，`assembleRelease` 会输出未签名 APK 并在日志中给出提示；若文件存在但字段不完整，构建会失败并提示缺少的字段。

请勿将以下内容提交到 Git：

```text
keystore.properties
*.jks
*.keystore
local.properties
```

默认情况下，开发者可以正常构建 Debug 版本。构建正式 Release APK 时，需要自行准备签名文件和相关配置。

## 项目结构

```text
app/src/main/java/com/mio/voice/
├── cache/         # 音频缓存
├── core/          # 核心逻辑（文本分段、生成指纹等）
├── data/          # 数据模型、本地存储与音色库
│   └── generation/    # 生成记录的数据库、DAO 与仓库
├── director/      # AI 文本分析（OpenAI 兼容）相关逻辑
├── export/        # 音频导出
├── playback/      # 音频播放控制
├── provider/      # TTS 服务适配（MiniMax 等）
├── ui/            # Compose 页面、组件与 ViewModel
└── MainActivity.kt
```

> 项目结构可能会随着后续开发继续调整。

## 开发计划

### 已完成

- [x] 基础文本转语音流程
- [x] MiniMax TTS 服务配置
- [x] 音色库
- [x] 父音色与子情绪预设
- [x] 长文本分段生成
- [x] 历史生成记录
- [x] 语音详情页面
- [x] 基础音频播放
- [x] TTS 与 AI 配置页面分离
- [x] Alpha APK 发布

### 正在完善

- [ ] 优化生成过程中的状态展示
- [ ] 完善错误提示与异常处理
- [ ] 优化音色库交互
- [ ] 完善音频保存与导出
- [ ] 优化不同尺寸设备的界面适配
- [ ] 完善应用视觉细节
- [ ] 增加更多自动化测试

### 未来计划

- [ ] 抽象统一的 TTS Provider 接口
- [ ] 接入更多云端 TTS 服务
- [ ] 增加音频分享功能
- [ ] 增加配置导入与导出
- [ ] 完善 AI 文本与情绪分析
- [ ] 增加更多语言支持
- [ ] 评估本地 TTS 服务连接方案
- [ ] 完善开发者文档
- [ ] 完善国际化支持

> 路线图会根据实际开发情况进行调整，不代表所有功能都一定会实现。

## 已知问题

当前 Alpha 版本可能存在以下问题：

- 部分错误提示不够清晰
- 不同 Android 系统上的界面表现可能存在差异
- 长文本生成速度取决于第三方服务
- 第三方服务接口变化可能导致相关功能暂时失效
- 部分页面和返回逻辑仍在持续优化
- Release 构建流程尚未经过大范围设备测试

发现问题时，欢迎通过 Issue 反馈。

## 参与贡献

欢迎提交 Issue、功能建议和 Pull Request。

目前比较需要帮助的方向包括：

- 新的云端 TTS Provider 适配
- Android 界面与交互优化
- 多设备兼容性测试
- 错误处理
- 音频播放、保存与导出
- 文档完善
- 英文及其他语言翻译

对于较大的功能修改，建议先创建 Issue 说明方案，避免重复开发或与项目方向产生冲突。

## 提交问题

提交 Bug 时，建议提供：

- Mio Voice 版本
- Android 版本
- 手机型号
- 使用的 TTS 服务
- 问题复现步骤
- 错误截图
- 已隐藏敏感信息的相关日志

请勿在 Issue 中提交：

- API Key
- 完整鉴权请求
- Keystore 文件或密码
- 其他个人隐私和敏感信息

## 开源协议

本项目采用 [MIT License](./LICENSE) 开源协议。

## 致谢

感谢以下项目和技术：

- Kotlin
- Jetpack Compose
- Android Jetpack（Room、DataStore、Lifecycle 等）
- Media3 (ExoPlayer)
- OkHttp
- Kotlin Coroutines

同时感谢所有参与测试、提出建议和提交反馈的用户。

## 免责声明

Mio Voice 是一个第三方开源客户端，与 MiniMax 以及其他 TTS、AI 服务提供商不存在官方隶属或合作关系。

用户需要自行申请、配置和使用第三方服务，并自行承担由此产生的费用和风险。

请勿使用本项目生成违法、侵权、欺诈、冒充他人或其他不当内容。使用者应遵守所在地区的法律法规，以及所使用第三方服务的相关协议。

---

<div align="center">

Made with Kotlin and Jetpack Compose.

</div>
