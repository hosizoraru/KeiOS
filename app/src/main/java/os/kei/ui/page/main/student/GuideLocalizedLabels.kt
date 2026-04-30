package os.kei.ui.page.main.student

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import os.kei.R

private fun guideLabelLookupKey(raw: String): String {
    return raw
        .trim()
        .replace(" ", "")
        .replace("　", "")
}

@StringRes
internal fun guideSourceLabelRes(raw: String): Int? {
    return when (guideLabelLookupKey(raw)) {
        "信息" -> R.string.guide_label_info
        "图片" -> R.string.guide_label_image
        "见下图" -> R.string.guide_label_see_image_below
        "技能" -> R.string.guide_label_skill
        "ex", "ex技能" -> R.string.guide_skill_type_ex
        "普通技能" -> R.string.guide_skill_type_normal
        "被动技能" -> R.string.guide_skill_type_passive
        "辅助技能" -> R.string.guide_skill_type_sub
        "状态" -> R.string.guide_label_status
        "分类" -> R.string.guide_label_category
        "等级" -> R.string.guide_label_level
        "稀有度", "星级" -> R.string.guide_field_rarity
        "学院", "学园", "所属学园", "所属学院" -> R.string.guide_field_school
        "所属社团", "社团" -> R.string.guide_field_club
        "武器类型" -> R.string.guide_field_weapon_type
        "战术位置作用", "战术作用", "作用" -> R.string.guide_field_tactical_role
        "攻击类型" -> R.string.guide_field_attack_type
        "防御类型" -> R.string.guide_field_defense_type
        "位置" -> R.string.guide_field_position
        "市街" -> R.string.guide_field_urban
        "屋外" -> R.string.guide_field_outdoor
        "屋内", "室内" -> R.string.guide_field_indoor
        "声优" -> R.string.guide_field_voice_actor
        "画师" -> R.string.guide_field_illustrator
        "生日" -> R.string.guide_field_birthday
        "年龄" -> R.string.guide_field_age
        "兴趣爱好" -> R.string.guide_field_hobbies
        "介绍", "个人简介" -> R.string.guide_field_intro
        "攻击力" -> R.string.guide_stat_attack
        "防御力" -> R.string.guide_stat_defense
        "生命值" -> R.string.guide_stat_hp
        "治愈力" -> R.string.guide_stat_healing
        "命中值" -> R.string.guide_stat_accuracy
        "闪避值" -> R.string.guide_stat_evasion
        "暴击值" -> R.string.guide_stat_crit
        "暴击伤害" -> R.string.guide_stat_crit_damage
        "稳定值" -> R.string.guide_stat_stability
        "射程" -> R.string.guide_stat_range
        "群控强化力" -> R.string.guide_stat_cc_power
        "群控抵抗力" -> R.string.guide_stat_cc_resist
        "装弹数" -> R.string.guide_stat_ammo
        "防御无视值" -> R.string.guide_stat_defense_ignore
        "受恢复率" -> R.string.guide_stat_healing_received
        "COST恢复力", "cost恢复力" -> R.string.guide_stat_cost_recovery
        "暴击抵抗值" -> R.string.guide_stat_crit_resist
        "暴伤抵抗率", "暴伤抵抗值", "暴击伤害抵抗率" -> R.string.guide_stat_crit_damage_resist
        "回忆大厅BGM", "回忆大厅bgm" -> R.string.ba_catalog_bgm_track_fallback
        "回忆大厅解锁等级" -> R.string.guide_gallery_memory_unlock_level
        "立绘" -> R.string.guide_gallery_standing_art
        "回忆大厅" -> R.string.guide_gallery_memorial_lobby
        "回忆大厅视频" -> R.string.guide_gallery_memorial_lobby_video
        "角色演示" -> R.string.guide_gallery_character_trailer
        "官方介绍" -> R.string.guide_gallery_official_intro
        "本家画" -> R.string.guide_gallery_original_art
        "官方衍生" -> R.string.guide_gallery_official_derivative
        "TV动画设定图" -> R.string.guide_gallery_tv_anime_design
        "设定集" -> R.string.guide_gallery_design_collection
        "巧克力图", "情人节巧克力" -> R.string.guide_gallery_chocolate_image
        "影画相关链接" -> R.string.guide_gallery_related_links
        "影画链接" -> R.string.guide_gallery_related_link
        "角色表情" -> R.string.guide_gallery_expression_title
        "学生昵称" -> R.string.guide_profile_section_nickname
        "学生信息" -> R.string.guide_profile_section_info
        "学生爱好" -> R.string.guide_profile_section_hobbies
        "礼物偏好" -> R.string.guide_profile_section_gifts
        "巧克力" -> R.string.guide_profile_section_chocolate
        "互动家具" -> R.string.guide_profile_section_furniture
        "礼物" -> R.string.guide_profile_gift
        "相关同名角色" -> R.string.guide_profile_related_same_name
        "同名角色" -> R.string.guide_profile_same_name
        "配音" -> R.string.guide_voice_dubbing
        "台词" -> R.string.guide_voice_line
        "语音条目" -> R.string.guide_voice_entry
        "成长" -> R.string.guide_voice_growth_section
        "专属武器" -> R.string.guide_weapon_unique
        "专武" -> R.string.guide_weapon_unique_short
        "专武数值" -> R.string.guide_weapon_stats
        "效果" -> R.string.guide_weapon_effect
        "辅助技能强化" -> R.string.guide_weapon_passive_skill_upgrade
        "角色能力" -> R.string.guide_simulate_character_stats
        "初始数据" -> R.string.guide_simulate_initial_stats
        "初始能力" -> R.string.guide_simulate_initial_stats
        "顶级数据" -> R.string.guide_simulate_max_stats
        "最大培养" -> R.string.guide_simulate_max_stats
        "角色能力说明" -> R.string.guide_simulate_character_stats_note
        "装备" -> R.string.guide_simulate_equipment
        "爱用品" -> R.string.guide_simulate_favorite_item
        "能力解放" -> R.string.guide_simulate_ability_release
        "羁绊等级奖励" -> R.string.guide_simulate_bond_rewards
        "羁绊角色" -> R.string.guide_simulate_bond_character
        else -> null
    }
}

@Composable
internal fun guideLocalizedLabel(
    raw: String,
    @StringRes fallbackRes: Int = R.string.guide_label_info
): String {
    val clean = raw.trim()
    val dynamicLabel = guideLocalizedDynamicLabel(clean)
    if (dynamicLabel != null) return dynamicLabel
    val labelRes = guideSourceLabelRes(clean)
    return when {
        labelRes != null -> stringResource(labelRes)
        clean.isBlank() -> stringResource(fallbackRes)
        else -> clean
    }
}

@Composable
private fun guideLocalizedDynamicLabel(raw: String): String? {
    val normalized = guideLabelLookupKey(raw)
    Regex("""^(\d+)号装备$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_simulate_equipment_slot_format, match.groupValues[1])
    }
    Regex("""^羁绊角色(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_simulate_bond_character_format, match.groupValues[1])
    }
    Regex("""^(\d+)级$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_label_level_format, match.groupValues[1])
    }
    Regex("""^T(\d+)效果$""", RegexOption.IGNORE_CASE).matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_simulate_favorite_item_tier_effect_format, match.groupValues[1])
    }
    Regex("""^T(\d+)所需升级材料$""", RegexOption.IGNORE_CASE).matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_simulate_favorite_item_tier_materials_format, match.groupValues[1])
    }
    Regex("""^T(\d+)技能图标$""", RegexOption.IGNORE_CASE).matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_simulate_favorite_item_tier_icon_format, match.groupValues[1])
    }
    Regex("""^立绘(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_standing_art_format, match.groupValues[1])
    }
    Regex("""^回忆大厅视频(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_memorial_lobby_video_format, match.groupValues[1])
    }
    Regex("""^角色演示(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_character_trailer_format, match.groupValues[1])
    }
    Regex("""^官方介绍(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_official_intro_format, match.groupValues[1])
    }
    Regex("""^互动家具(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_interactive_furniture_format, match.groupValues[1])
    }
    Regex("""^(巧克力图|情人节巧克力)(\d+)$""").matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_chocolate_image_format, match.groupValues[2])
    }
    Regex("""^PV(\d+)$""", RegexOption.IGNORE_CASE).matchEntire(normalized)?.let { match ->
        return stringResource(R.string.guide_gallery_pv_format, match.groupValues[1])
    }
    return null
}

@Composable
internal fun guideLocalizedValue(raw: String): String {
    return when (raw.trim()) {
        "暂无数据", "原网站暂无该数据" -> stringResource(R.string.guide_value_no_data)
        "暂无更多参数，可点击来源查看完整图鉴。" -> stringResource(R.string.guide_value_open_source_for_full_guide)
        "未解析到详细描述，可点击下方来源查看完整图鉴。" -> stringResource(R.string.guide_value_description_not_parsed)
        else -> raw
    }
}

@Composable
internal fun guideLocalizedVoiceLanguage(raw: String): String {
    val normalized = guideLabelLookupKey(raw).lowercase()
    val languageMatch = Regex("""^语言(\d+)$""").matchEntire(normalized)
    return when {
        languageMatch != null -> stringResource(
            R.string.guide_voice_language_format,
            languageMatch.groupValues[1].toIntOrNull() ?: 1
        )
        normalized == "日配" -> stringResource(R.string.guide_voice_language_jp)
        normalized == "中配" -> stringResource(R.string.guide_voice_language_cn)
        normalized == "韩配" -> stringResource(R.string.guide_voice_language_kr)
        normalized == "官翻" -> stringResource(R.string.guide_voice_language_official)
        else -> raw.trim()
    }
}

@Composable
internal fun guideLocalizedVoiceLineLabel(raw: String, fallbackIndex: Int): String {
    val clean = raw.trim()
    val normalized = guideLabelLookupKey(clean)
    val lineMatch = Regex("""^台词(\d+)$""").matchEntire(normalized)
    return when {
        normalized == "台词" -> stringResource(R.string.guide_voice_line)
        lineMatch != null -> stringResource(
            R.string.guide_voice_line_format,
            lineMatch.groupValues[1].toIntOrNull() ?: fallbackIndex + 1
        )
        clean.isBlank() -> stringResource(R.string.guide_voice_line_format, fallbackIndex + 1)
        else -> guideLocalizedVoiceLanguage(clean).ifBlank { clean }
    }
}

@Composable
internal fun guideLocalizedVoiceEntryTitle(raw: String): String {
    val clean = raw.trim()
    val normalized = guideLabelLookupKey(clean)
    val growthMatch = Regex("""^成长台词(\d+)$""").matchEntire(normalized)
    return when {
        growthMatch != null -> stringResource(
            R.string.guide_voice_growth_title_format,
            growthMatch.groupValues[1].toIntOrNull() ?: 1
        )
        clean.isBlank() -> ""
        else -> guideLocalizedLabel(clean)
    }
}
