package hoon.example.androidsandbox.presentation.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import hoon.example.androidsandbox.data.counter.repository.NameRepositoryImpl
import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.domain.counter.repository.NameRepository
import hoon.example.androidsandbox.domain.counter.usecase.AddNameUseCase
import hoon.example.androidsandbox.domain.counter.usecase.GetNamesUseCase
import hoon.example.androidsandbox.domain.counter.usecase.RemoveNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CounterViewModel(
    private val getNamesUseCase: GetNamesUseCase,
    private val addNameUseCase: AddNameUseCase,
    private val removeNameUseCase: RemoveNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    init {
        observeNames()
    }

    private fun observeNames() {
        viewModelScope.launch {
            getNamesUseCase().collect { names ->
                _uiState.update { state ->
                    state.copy(
                        nameList = names,
                        isGoalReached = names.size >= 10
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(inputName = name) }
    }

    fun addName() {
        viewModelScope.launch {
            val success = addNameUseCase(_uiState.value.inputName)
            if (success) {
                _uiState.update { it.copy(inputName = "") }
            }
        }
    }

    fun removeName(name: Name) {
        viewModelScope.launch {
            removeNameUseCase(name)
        }
    }

    companion object {
        fun provideFactory(
            repository: NameRepository = NameRepositoryImpl()
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CounterViewModel(
                    getNamesUseCase = GetNamesUseCase(repository),
                    addNameUseCase = AddNameUseCase(repository),
                    removeNameUseCase = RemoveNameUseCase(repository)
                ) as T
            }
        }
    }
}
