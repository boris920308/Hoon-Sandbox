package hoon.example.androidsandbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class CounterViewModel : ViewModel() {
    var name by mutableStateOf("")
    val nameList = mutableStateListOf<String>()

    // 비즈니스 로직 : List에 추가
    fun addName() {
        if (name.isNotBlank()) {
            nameList.add(name)
            name = ""
        }
    }

    fun removeName(targetName: String) {
        nameList.remove(targetName)
    }
}