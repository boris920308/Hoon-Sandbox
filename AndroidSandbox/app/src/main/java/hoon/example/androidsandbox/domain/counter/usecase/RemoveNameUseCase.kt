package hoon.example.androidsandbox.domain.counter.usecase

import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.domain.counter.repository.NameRepository
import javax.inject.Inject

class RemoveNameUseCase @Inject constructor(
    private val repository: NameRepository
) {
    suspend operator fun invoke(name: Name) {
        repository.removeName(name)
    }
}
