package hoon.example.androidsandbox.data.counter.repository

import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.domain.counter.repository.NameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class NameRepositoryImpl @Inject constructor() : NameRepository {

    private val _names = MutableStateFlow<List<Name>>(emptyList())

    override fun getNames(): Flow<List<Name>> {
        return _names.asStateFlow()
    }

    override suspend fun addName(name: Name) {
        _names.update { currentList ->
            currentList + name
        }
    }

    override suspend fun removeName(name: Name) {
        _names.update { currentList ->
            currentList.filter { it != name }
        }
    }
}
