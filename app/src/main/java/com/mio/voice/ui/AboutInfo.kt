package com.mio.voice.ui

/**
 * 「关于 Mio Voice」页面的纯文本与展示逻辑（不依赖 Compose），便于单测覆盖。
 *
 * - 版本号格式化：保证从 BuildConfig.VERSION_NAME 读取后不出现重复的 v 前缀。
 * - 许可证 / 隐私文案：内置为常量，保证离线可读（不依赖网络，也不在运行期读取打包外文件）。
 */
object AboutInfo {

    const val GITHUB_URL = "https://github.com/miofling/mio-voice"
    const val ISSUES_URL = "https://github.com/miofling/mio-voice/issues"

    /**
     * 展示版本号。若原始版本名已带 v/V 前缀则不重复添加，避免出现 "vv0.1.0"。
     */
    fun formatVersionName(raw: String): String =
        if (raw.startsWith("v") || raw.startsWith("V")) raw else "v$raw"

    /** 项目根目录 LICENSE 中的 MIT License 全文（与 LICENSE 文件保持一致）。 */
    val LICENSE_TEXT: String = """MIT License

Copyright (c) 2026 miofling

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE."""

    /** 隐私说明分段文案：与当前实际实现一致，自然易懂。 */
    val PRIVACY_PARAGRAPHS: List<String> = listOf(
        "Mio Voice 不会主动把你的 API Key、应用设置或本地生成记录上传给项目开发者。",
        "你的 API Key 和相关配置只保存在本设备本地。",
        "当你发起语音生成或 AI 分析时，输入的文本、音色参数及必要数据会发送到你自行配置的第三方服务。",
        "这些第三方服务如何存储和处理数据，由对应服务商的隐私政策决定。",
        "请自行确认所使用服务商的数据政策，不要提交敏感或你无权处理的内容。",
        "当前版本不包含开发者自建的账号系统、云同步或用户行为统计功能。"
    )
}
