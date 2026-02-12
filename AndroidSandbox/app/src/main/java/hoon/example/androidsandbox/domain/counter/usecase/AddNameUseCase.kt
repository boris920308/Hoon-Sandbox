package hoon.example.androidsandbox.domain.counter.usecase

import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.domain.counter.repository.NameRepository

class AddNameUseCase(
    private val repository: NameRepository
) {
    suspend operator fun invoke(name: String): Boolean {
        if (name.isBlank()) return false
        repository.addName(Name(name))
        return true
    }
}
