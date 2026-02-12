package hoon.example.androidsandbox.domain.kvs.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import org.webrtc.EglBase
import org.webrtc.VideoSink

interface KvsMasterRepository {
    val connectionState: Flow<KvsMasterConnectionState>

    fun initialize(context: Context)
    fun getEglBaseContext(): EglBase.Context?
    fun setLocalVideoSink(sink: VideoSink)
    fun startLocalVideo(context: Context, isFrontCamera: Boolean)
    fun switchCamera()
    suspend fun connect(): Result<Unit>
    fun disconnect()
    fun release()
}

enum class KvsMasterConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
