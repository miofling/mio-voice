package com.mio.voice.director

interface DirectorProvider {
    suspend fun analyze(request: DirectorRequest): DirectorResult
}

