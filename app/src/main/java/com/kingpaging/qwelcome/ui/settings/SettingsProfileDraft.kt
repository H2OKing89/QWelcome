package com.kingpaging.qwelcome.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kingpaging.qwelcome.data.TechProfile

private const val PROFILE_FIELD_COUNT = 3

/** A restored draft keeps edited fields while untouched fields follow persisted changes. */
internal class SettingsProfileDraft(
    baseline: TechProfile,
    edited: TechProfile = baseline,
) {
    private var baseline by mutableStateOf(baseline)
    var name by mutableStateOf(edited.name)
    var title by mutableStateOf(edited.title)
    var department by mutableStateOf(edited.dept)

    fun toProfile(): TechProfile = TechProfile(name, title, department)

    fun updatePersisted(profile: TechProfile) {
        if (baseline == profile) return
        if (name == baseline.name) name = profile.name
        if (title == baseline.title) title = profile.title
        if (department == baseline.dept) department = profile.dept
        baseline = profile
    }

    companion object {
        val Saver =
            listSaver<SettingsProfileDraft, String>(
                save = {
                    listOf(it.baseline.name, it.baseline.title, it.baseline.dept, it.name, it.title, it.department)
                },
                restore = {
                    val original = it.take(PROFILE_FIELD_COUNT)
                    val edited = it.drop(PROFILE_FIELD_COUNT)
                    SettingsProfileDraft(
                        baseline = TechProfile(original[0], original[1], original[2]),
                        edited = TechProfile(edited[0], edited[1], edited[2]),
                    )
                },
            )
    }
}

@Composable
internal fun rememberSettingsProfileDraft(profile: TechProfile): SettingsProfileDraft {
    val draft = rememberSaveable(saver = SettingsProfileDraft.Saver) { SettingsProfileDraft(profile) }
    SideEffect { draft.updatePersisted(profile) }
    return draft
}
