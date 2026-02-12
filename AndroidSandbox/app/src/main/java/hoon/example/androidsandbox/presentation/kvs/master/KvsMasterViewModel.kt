package hoon.example.androidsandbox.presentation.kvs.master

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hoon.example.androidsandbox.BuildConfig
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterConnectionState
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoSink
import javax.inject.Inject

@HiltViewModel
class KvsMasterViewModel @Inject constructor(
    private val repository: KvsMasterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        KvsMasterUiState(channelName = BuildConfig.KVS_CHANNEL_NAME)
    )
    val uiState: StateFlow<KvsMasterUiState> = _uiState.asStateFlow()

    private var isInitialized = false

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update {
                    it.copy(connectionState = state.toUiState())
                }
            }
        }
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        repository.initialize(context)
        isInitialized = true
    }

    fun getEglBaseContext(): EglBase.Context? {
        return repository.getEglBaseContext()
    }

    fun setLocalVideoSink(sink: VideoSink) {
        repository.setLocalVideoSink(sink)
    }

    fun startLocalVideo(context: Context) {
        val isFrontCamera = _uiState.value.cameraFacing == CameraFacing.FRONT
        repository.startLocalVideo(context, isFrontCamera)
    }

    fun connect() {
        viewModelScope.launch {
            repository.connect()
        }
    }

    fun disconnect() {
        repository.disconnect()
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
        repository.switchCamera()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }

    private fun KvsMasterConnectionState.toUiState(): ConnectionState {
        return when (this) {
            KvsMasterConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
            KvsMasterConnectionState.CONNECTING -> ConnectionState.CONNECTING
            KvsMasterConnectionState.CONNECTED -> ConnectionState.CONNECTED
            KvsMasterConnectionState.ERROR -> ConnectionState.ERROR
        }
    }
}
