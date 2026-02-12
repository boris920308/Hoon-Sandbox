package hoon.example.androidsandbox.domain.counter.usecase

import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.domain.counter.repository.NameRepository
import kotlinx.coroutines.flow.Flow

class GetNamesUseCase(
    private val repository: NameRepository
) {
    operator fun invoke(): Flow<List<Name>> {
        return repository.getNames()
    }
}
