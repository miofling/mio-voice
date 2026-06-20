package com.mio.voice.director

interface DirectorProvider {
    suspend fun analyze(request: DirectorRequest): DirectorResult

    /** 拉取 OpenAI 兼容服务的可用模型列表（GET /v1/models）。 */
    suspend fun fetchModels(config: com.mio.voice.data.DirectorConfig): List<String>
}

