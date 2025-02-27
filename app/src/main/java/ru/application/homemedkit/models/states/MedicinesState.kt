package ru.application.homemedkit.models.states

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import ru.application.homemedkit.data.dto.Medicine
import ru.application.homemedkit.helpers.BLANK
import ru.application.homemedkit.helpers.Preferences
import ru.application.homemedkit.helpers.Sorting

data class MedicinesState(
    val search: String = BLANK,
    val sorting: Comparator<Medicine> =
        Sorting.entries.find { it.value == Preferences.getSortingOrder() }?.type ?: Sorting.IN_NAME.type,
    val kits: SnapshotStateList<Long> = mutableStateListOf(),
    val showSort: Boolean = false,
    val showFilter: Boolean = false,
    val showAdding: Boolean = false,
    val showExit: Boolean = false,
    val listState: LazyListState = LazyListState()
)