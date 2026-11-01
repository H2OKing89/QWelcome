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
    private var nameEdited: Boolean = edited.name != baseline.name,
    private var titleEdited: Boolean = edited.title != baseline.title,
    private var departmentEdited: Boolean = edited.dept != baseline.dept,
) {
    private var baseline by mutableStateOf(baseline)
    private var profile by mutableStateOf(edited)

    var name: String
        get() = profile.name
        set(value) {
            nameEdited = true
            profile = profile.copy(name = value)
        }

    var title: String
        get() = profile.title
        set(value) {
            titleEdited = true
            profile = profile.copy(title = value)
        }

    var department: String
        get() = profile.dept
        set(value) {
            departmentEdited = true
            profile = profile.copy(dept = value)
        }

    fun toProfile(): TechProfile = profile

    fun updatePersisted(profile: TechProfile) {
        if (baseline == profile) return
        this.profile =
            TechProfile(
                name = if (nameEdited) name else profile.name,
                title = if (titleEdited) title else profile.title,
                dept = if (departmentEdited) department else profile.dept,
            )
        baseline = profile
    }

    companion object {
        val Saver =
            listSaver<SettingsProfileDraft, String>(
                save = {
                    listOf(
                        it.baseline.name,
                        it.baseline.title,
                        it.baseline.dept,
                        it.name,
                        it.title,
                        it.department,
                        it.nameEdited.toString(),
                        it.titleEdited.toString(),
                        it.departmentEdited.toString(),
                    )
                },
                restore = {
                    val original = it.take(PROFILE_FIELD_COUNT)
                    val edited = it.drop(PROFILE_FIELD_COUNT)
                    val editedFields = it.drop(PROFILE_FIELD_COUNT * 2)
                    SettingsProfileDraft(
                        baseline = TechProfile(original[0], original[1], original[2]),
                        edited = TechProfile(edited[0], edited[1], edited[2]),
                        nameEdited = editedFields[0].toBoolean(),
                        titleEdited = editedFields[1].toBoolean(),
                        departmentEdited = editedFields[2].toBoolean(),
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
