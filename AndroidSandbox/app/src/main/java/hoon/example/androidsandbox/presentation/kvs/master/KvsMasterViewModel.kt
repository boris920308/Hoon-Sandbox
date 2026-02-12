package hoon.example.androidsandbox.presentation.kvs.master

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import hoon.example.androidsandbox.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class KvsMasterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        KvsMasterUiState(channelName = BuildConfig.KVS_CHANNEL_NAME)
    )
    val uiState: StateFlow<KvsMasterUiState> = _uiState.asStateFlow()

    fun connect() {
        _uiState.update { it.copy(connectionState = ConnectionState.CONNECTING) }
        // TODO: 실제 KVS 연결 로직
    }

    fun disconnect() {
        _uiState.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
        // TODO: 실제 KVS 연결 해제 로직
    }

    fun toggleCamera() {
        _uiState.update { state ->
            state.copy(
                cameraFacing = if (state.cameraFacing == CameraFacing.FRONT) {
                    CameraFacing.BACK
                } else {
                    CameraFacing.FRONT
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
