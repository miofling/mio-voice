package com.mio.voice.data.generation

/** 组名校验与归一（纯逻辑，便于单测）。 */
object CollectionFormLogic {
    const val MAX_NAME_LENGTH = 40

    sealed interface NameResult {
        data class Valid(val name: String) : NameResult
        data class Invalid(val message: String) : NameResult
    }

    /** trim 后校验：非空、不超长。返回归一后的合法名或错误信息。 */
    fun validateName(raw: String): NameResult {
        val name = raw.trim()
        return when {
            name.isEmpty() -> NameResult.Invalid("请填写组名。")
            name.length > MAX_NAME_LENGTH -> NameResult.Invalid("组名过长（最多 $MAX_NAME_LENGTH 字）。")
            else -> NameResult.Valid(name)
        }
    }
}
