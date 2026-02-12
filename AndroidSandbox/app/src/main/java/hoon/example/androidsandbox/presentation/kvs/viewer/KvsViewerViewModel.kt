package hoon.example.androidsandbox.presentation.kvs.viewer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hoon.example.androidsandbox.BuildConfig
import hoon.example.androidsandbox.domain.kvs.repository.KvsViewerConnectionState
import hoon.example.androidsandbox.domain.kvs.repository.KvsViewerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoSink
import javax.inject.Inject

@HiltViewModel
class KvsViewerViewModel @Inject constructor(
    private val repository: KvsViewerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        KvsViewerUiState(channelName = BuildConfig.KVS_CHANNEL_NAME)
    )
    val uiState: StateFlow<KvsViewerUiState> = _uiState.asStateFlow()

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
        repository.initialize(context)
    }

    fun getEglBaseContext(): EglBase.Context? {
        return repository.getEglBaseContext()
    }

    fun setRemoteVideoSink(sink: VideoSink) {
        repository.setRemoteVideoSink(sink)
    }

    fun connect() {
        viewModelScope.launch {
            repository.connect()
        }
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }

    private fun KvsViewerConnectionState.toUiState(): ViewerConnectionState {
        return when (this) {
            KvsViewerConnectionState.DISCONNECTED -> ViewerConnectionState.DISCONNECTED
            KvsViewerConnectionState.CONNECTING -> ViewerConnectionState.CONNECTING
            KvsViewerConnectionState.CONNECTED -> ViewerConnectionState.CONNECTED
            KvsViewerConnectionState.ERROR -> ViewerConnectionState.ERROR
        }
    }
}
