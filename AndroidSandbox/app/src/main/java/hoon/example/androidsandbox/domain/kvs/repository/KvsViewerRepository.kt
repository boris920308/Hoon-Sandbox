package hoon.example.androidsandbox.domain.kvs.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.webrtc.EglBase
import org.webrtc.VideoSink

interface KvsViewerRepository {
    val connectionState: Flow<KvsViewerConnectionState>

    fun initialize(context: Context)
    fun getEglBaseContext(): EglBase.Context?
    fun setRemoteVideoSink(sink: VideoSink)
    suspend fun connect(): Result<Unit>
    fun disconnect()
    fun release()
}

enum class KvsViewerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
