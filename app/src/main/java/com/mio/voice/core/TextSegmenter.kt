package com.mio.voice.core

class TextSegmenter(
    private val maxChars: Int = 240
) {
    fun split(input: String): List<String> {
        val normalized = input.replace("\r\n", "\n").replace('\r', '\n')
        if (normalized.isBlank()) return emptyList()

        val primary = splitBySentenceEnd(normalized)
        return primary.flatMap { splitLong(it.trim()) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitBySentenceEnd(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            current.append(char)

            val isEllipsis = char == '.' && text.startsWith("...", index)
            if (isEllipsis) {
                current.append("..")
                index += 2
                index = consumeClosers(text, index + 1, current) - 1
                addCurrent(result, current)
            } else if (char == '\u2026' && index + 1 < text.length && text[index + 1] == '\u2026') {
                current.append('\u2026')
                index += 1
                index = consumeClosers(text, index + 1, current) - 1
                addCurrent(result, current)
            } else if (char in sentenceEndChars) {
                index = consumeClosers(text, index + 1, current) - 1
                addCurrent(result, current)
            } else if (char == '\n' && current.endsWithBlankLine()) {
                addCurrent(result, current)
            }
            index++
        }
        addCurrent(result, current)
        return result
    }

    private fun consumeClosers(text: String, start: Int, current: StringBuilder): Int {
        var i = start
        while (i < text.length && text[i] in closingChars) {
            current.append(text[i])
            i++
        }
        return i
    }

    private fun addCurrent(result: MutableList<String>, current: StringBuilder) {
        val value = current.toString().trim()
        if (value.isNotEmpty()) result += value
        current.clear()
    }

    private fun StringBuilder.endsWithBlankLine(): Boolean {
        if (length < 2) return false
        var newlines = 0
        var i = length - 1
        while (i >= 0 && this[i].isWhitespace()) {
            if (this[i] == '\n') newlines++
            if (newlines >= 2) return true
            i--
        }
        return false
    }

    private fun splitLong(text: String): List<String> {
        if (text.length <= maxChars) return listOf(text)
        return splitLongByDelimiters(text, listOf('\n'), keepDelimiter = false)
            .flatMap { splitLongByDelimiters(it, listOf(';', '；'), keepDelimiter = true) }
            .flatMap { splitLongByDelimiters(it, listOf(',', '，', '、'), keepDelimiter = true) }
            .flatMap { splitLongByDelimiters(it, listOf(' ', '\t'), keepDelimiter = true) }
            .flatMap { splitBySafeLength(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitLongByDelimiters(
        text: String,
        delimiters: List<Char>,
        keepDelimiter: Boolean
    ): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        text.forEach { char ->
            if (char in delimiters) {
                if (keepDelimiter) current.append(char)
                flushChunk(pieces, current)
                if (!keepDelimiter && char == '\n') {
                    // Drop only separator newlines introduced as split points.
                }
            } else {
                current.append(char)
            }
        }
        flushChunk(pieces, current)
        return packPieces(pieces)
    }

    private fun flushChunk(pieces: MutableList<String>, current: StringBuilder) {
        val value = current.toString().trim()
        if (value.isNotEmpty()) pieces += value
        current.clear()
    }

    private fun packPieces(pieces: List<String>): List<String> {
        val packed = mutableListOf<String>()
        val current = StringBuilder()
        pieces.forEach { piece ->
            if (current.isEmpty()) {
                current.append(piece)
            } else if (current.length + piece.length <= maxChars) {
                current.append(piece)
            } else {
                packed += current.toString()
                current.clear()
                current.append(piece)
            }
        }
        if (current.isNotEmpty()) packed += current.toString()
        return packed
    }

    private fun splitBySafeLength(text: String): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val result = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = (start + maxChars).coerceAtMost(text.length)
            result += text.substring(start, end)
            start = end
        }
        return result
    }

    private companion object {
        val sentenceEndChars = setOf('。', '！', '？', '.', '!', '?')
        val closingChars = setOf(
            '"', '\'', '”', '’', '」', '』', '）', ')', ']', '】', '》', '〉'
        )
    }
}
