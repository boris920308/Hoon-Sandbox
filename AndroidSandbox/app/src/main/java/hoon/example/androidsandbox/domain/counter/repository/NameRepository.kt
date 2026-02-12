package hoon.example.androidsandbox.domain.counter.repository

import hoon.example.androidsandbox.domain.counter.model.Name
import kotlinx.coroutines.flow.Flow

interface NameRepository {
    fun getNames(): Flow<List<Name>>
    suspend fun addName(name: Name)
    suspend fun removeName(name: Name)
}
