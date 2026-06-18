package com.mio.voice.provider

import com.mio.voice.data.ProviderConfig
import com.mio.voice.data.TtsRequest
import com.mio.voice.data.TtsResult

interface TtsProvider {
    suspend fun generate(request: TtsRequest): TtsResult
    suspend fun testConnection(config: ProviderConfig): TtsResult
}
