package com.mio.voice.provider

import com.mio.voice.provider.OfficialVoiceClassifier.Gender
import com.mio.voice.provider.OfficialVoiceClassifier.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class OfficialVoiceClassifierTest {

    private fun voice(id: String, name: String = "") = OfficialVoice(id, name)

    @Test
    fun detectsLanguageFromPrefix() {
        assertEquals(Language.Chinese, OfficialVoiceClassifier.language(voice("Chinese (Mandarin)_News_Anchor")))
        assertEquals(Language.Chinese, OfficialVoiceClassifier.language(voice("Chinese_Sweet_Lady")))
        assertEquals(Language.Cantonese, OfficialVoiceClassifier.language(voice("Cantonese_GentleLady")))
        assertEquals(Language.English, OfficialVoiceClassifier.language(voice("English_Trustworthy_Man")))
        assertEquals(Language.Japanese, OfficialVoiceClassifier.language(voice("Japanese_KindLady")))
        assertEquals(Language.Korean, OfficialVoiceClassifier.language(voice("Korean_SweetGirl")))
    }

    @Test
    fun legacyPinyinVoicesAreChinese() {
        assertEquals(Language.Chinese, OfficialVoiceClassifier.language(voice("male-qn-qingse")))
        assertEquals(Language.Chinese, OfficialVoiceClassifier.language(voice("female-shaonv")))
    }

    @Test
    fun chineseNameFallsBackToChinese() {
        assertEquals(Language.Chinese, OfficialVoiceClassifier.language(voice("clever_boy", "聪明男孩")))
    }

    @Test
    fun unknownPrefixIsOtherLanguage() {
        assertEquals(Language.Other, OfficialVoiceClassifier.language(voice("Robot_Armor", "Robot")))
        assertEquals(Language.Other, OfficialVoiceClassifier.language(voice("clever_boy", "Clever Boy")))
    }

    @Test
    fun detectsGenderFromLegacyPrefix() {
        assertEquals(Gender.Male, OfficialVoiceClassifier.gender(voice("male-qn-qingse")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.gender(voice("female-shaonv")))
    }

    @Test
    fun detectsGenderFromKeywords() {
        assertEquals(Gender.Male, OfficialVoiceClassifier.gender(voice("English_Trustworthy_Man")))
        assertEquals(Gender.Male, OfficialVoiceClassifier.gender(voice("Chinese (Mandarin)_Gentleman")))
        // 关键：Lady / Woman 不应被内含的 "man" 误判为男声。
        assertEquals(Gender.Female, OfficialVoiceClassifier.gender(voice("English_Graceful_Lady")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.gender(voice("Spanish_SereneWoman")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.gender(voice("Korean_SweetGirl")))
    }

    @Test
    fun chineseNameGenderFallback() {
        assertEquals(Gender.Female, OfficialVoiceClassifier.gender(voice("custom_1", "御姐")))
        assertEquals(Gender.Male, OfficialVoiceClassifier.gender(voice("custom_2", "大叔")))
    }

    @Test
    fun unknownGenderIsOther() {
        assertEquals(Gender.Other, OfficialVoiceClassifier.gender(voice("Robot_Armor", "Robot Armor")))
        assertEquals(Gender.Other, OfficialVoiceClassifier.gender(voice("Ghost", "Ghost")))
    }

    // ---- 高置信判定（缩减音色库用）----

    @Test
    fun confidentGenderTrustsLegacyPrefix() {
        assertEquals(Gender.Male, OfficialVoiceClassifier.confidentGender(voice("male-qn-qingse")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.confidentGender(voice("female-shaonv")))
    }

    @Test
    fun confidentGenderMatchesWholeRoleWords() {
        assertEquals(Gender.Male, OfficialVoiceClassifier.confidentGender(voice("English_Trustworthy_Man")))
        assertEquals(Gender.Male, OfficialVoiceClassifier.confidentGender(voice("Chinese (Mandarin)_Gentleman")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.confidentGender(voice("English_Graceful_Lady")))
    }

    @Test
    fun confidentGenderSplitsCamelCase() {
        // 粘连命名应能拆出角色词。
        assertEquals(Gender.Female, OfficialVoiceClassifier.confidentGender(voice("Spanish_SereneWoman")))
        assertEquals(Gender.Female, OfficialVoiceClassifier.confidentGender(voice("Korean_SweetGirl")))
        assertEquals(Gender.Male, OfficialVoiceClassifier.confidentGender(voice("English_WiseOldMan")))
    }

    @Test
    fun confidentGenderRejectsSubstringFalsePositives() {
        // "Mentor" 含 men 子串、"Salesman" 含 man 子串，但整词不在白名单，不应误判。
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("English_Mentor")))
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("English_Salesman")))
        // 过泛词已剔除：单纯 Boss / Guy 不再判性别。
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("English_BigBoss")))
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("English_CoolGuy")))
    }

    @Test
    fun confidentGenderIgnoresChineseNameFallback() {
        // 严格口径：纯中文名兜底不算高置信。
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("custom_1", "御姐")))
        assertEquals(null, OfficialVoiceClassifier.confidentGender(voice("custom_2", "大叔")))
    }

    @Test
    fun confidentLanguageOnlyTrustsPrefix() {
        assertEquals(Language.English, OfficialVoiceClassifier.confidentLanguage(voice("English_Trustworthy_Man")))
        assertEquals(Language.Chinese, OfficialVoiceClassifier.confidentLanguage(voice("male-qn-qingse")))
        // 纯中文名兜底不算高置信。
        assertEquals(null, OfficialVoiceClassifier.confidentLanguage(voice("clever_boy", "聪明男孩")))
    }

    @Test
    fun confidentVoicesKeepsClearOnesDropsFuzzy() {
        val input = listOf(
            voice("male-qn-qingse"),
            voice("female-shaonv"),
            voice("English_Trustworthy_Man"),
            voice("Spanish_SereneWoman"),
            voice("Korean_SweetGirl"),
            // 丢弃：无语言前缀（语言不可信）
            voice("clever_boy", "聪明男孩"),
            // 丢弃：性别不可信（泛词 / 子串误命中词）
            voice("English_Mentor"),
            voice("English_CoolGuy"),
            // 丢弃：拟物/角色音，无强信号
            voice("Robot_Armor", "Robot Armor"),
            voice("Ghost", "Ghost"),
            // 丢弃：纯中文名兜底
            voice("custom_1", "御姐")
        )
        val kept = OfficialVoiceClassifier.confidentVoices(input).map { it.voiceId }
        assertEquals(
            listOf("male-qn-qingse", "female-shaonv", "English_Trustworthy_Man", "Spanish_SereneWoman", "Korean_SweetGirl"),
            kept
        )
    }
}
