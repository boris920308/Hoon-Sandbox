package hoon.example.androidsandbox.presentation.counter

import hoon.example.androidsandbox.domain.counter.model.Name

data class CounterUiState(
    val inputName: String = "",
    val nameList: List<Name> = emptyList(),
    val isGoalReached: Boolean = false
) {
    val count: Int get() = nameList.size
}
