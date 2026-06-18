package com.mio.voice.core

class TechnicalTextChunker(
    private val maxChars: Int
) {
    fun split(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        if (input.length <= maxChars) return listOf(input)

        return paragraphUnits(input)
            .flatMap { splitOversizedUnit(it) }
            .let { pack(it) }
            .filter { it.isNotBlank() }
    }

    private fun paragraphUnits(input: String): List<String> {
        val units = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            current.append(c)
            if (c == '\n') {
                var j = i + 1
                while (j < input.length && (input[j] == ' ' || input[j] == '\t')) {
                    current.append(input[j])
                    j++
                }
                if (j < input.length && input[j] == '\n') {
                    current.append(input[j])
                    units += current.toString()
                    current.clear()
                    i = j
                }
            }
            i++
        }
        if (current.isNotEmpty()) units += current.toString()
        return units
    }

    private fun splitOversizedUnit(unit: String): List<String> {
        if (unit.length <= maxChars) return listOf(unit)
        return splitBySingleNewline(unit)
            .flatMap { splitByWhitespace(it) }
            .flatMap { splitBySafeLength(it) }
    }

    private fun splitBySingleNewline(text: String): List<String> {
        if (text.length <= maxChars) return listOf(text)
        return splitKeepingDelimiter(text, '\n')
    }

    private fun splitByWhitespace(text: String): List<String> {
        if (text.length <= maxChars) return listOf(text)
        val result = mutableListOf<String>()
        val current = StringBuilder()
        text.forEach { c ->
            current.append(c)
            if ((c == ' ' || c == '\t') && current.length >= maxChars) {
                result += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }

    private fun splitKeepingDelimiter(text: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        text.forEach { c ->
            current.append(c)
            if (c == delimiter && current.length >= maxChars) {
                result += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
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

    private fun pack(units: List<String>): List<String> {
        val packed = mutableListOf<String>()
        val current = StringBuilder()
        units.forEach { unit ->
            if (current.isEmpty()) {
                current.append(unit)
            } else if (current.length + unit.length <= maxChars) {
                current.append(unit)
            } else {
                packed += current.toString()
                current.clear()
                current.append(unit)
            }
        }
        if (current.isNotEmpty()) packed += current.toString()
        return packed
    }
}
