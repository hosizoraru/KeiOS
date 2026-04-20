package com.example.keios.ui.page.main.os.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.keios.ui.page.main.os.SectionKind
import com.example.keios.ui.page.main.os.SectionState

internal data class OsPageSectionStateStore(
    val sectionStates: Map<SectionKind, SectionState>,
    val onSectionStatesChange: (Map<SectionKind, SectionState>) -> Unit,
    val updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit
)

@Composable
internal fun rememberOsPageSectionStateStore(): OsPageSectionStateStore {
    var sectionStates by remember {
        mutableStateOf(
            mapOf(
                SectionKind.SYSTEM to SectionState(),
                SectionKind.SECURE to SectionState(),
                SectionKind.GLOBAL to SectionState(),
                SectionKind.ANDROID to SectionState(),
                SectionKind.JAVA to SectionState(),
                SectionKind.LINUX to SectionState()
            )
        )
    }

    val updateSection: (SectionKind, (SectionState) -> SectionState) -> Unit = { section, transform ->
        sectionStates = sectionStates.toMutableMap().also { map ->
            val old = map[section] ?: SectionState()
            map[section] = transform(old)
        }
    }

    return remember(sectionStates) {
        OsPageSectionStateStore(
            sectionStates = sectionStates,
            onSectionStatesChange = { sectionStates = it },
            updateSection = updateSection
        )
    }
}
