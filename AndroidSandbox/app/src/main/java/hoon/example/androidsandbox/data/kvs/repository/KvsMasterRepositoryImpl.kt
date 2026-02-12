package hoon.example.androidsandbox.data.kvs.repository

import android.content.Context
import hoon.example.androidsandbox.data.kvs.KvsConnectionState
import hoon.example.androidsandbox.data.kvs.KvsWebRtcClient
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterConnectionState
import hoon.example.androidsandbox.domain.kvs.repository.KvsMasterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.webrtc.EglBase
import org.webrtc.VideoSink
import javax.inject.Inject

class KvsMasterRepositoryImpl @Inject constructor(
    private val kvsWebRtcClient: KvsWebRtcClient
) : KvsMasterRepository {

    override val connectionState: Flow<KvsMasterConnectionState>
        get() = kvsWebRtcClient.connectionState.map { it.toDomain() }

    override fun initialize(context: Context) {
        kvsWebRtcClient.initializePeerConnectionFactory(context)
    }

    override fun getEglBaseContext(): EglBase.Context? {
        return kvsWebRtcClient.getEglBaseContext()
    }

    override fun setLocalVideoSink(sink: VideoSink) {
        kvsWebRtcClient.setLocalVideoSink(sink)
    }

    override fun startLocalVideo(context: Context, isFrontCamera: Boolean) {
        kvsWebRtcClient.startLocalVideo(context, isFrontCamera)
    }

    override fun switchCamera() {
        kvsWebRtcClient.switchCamera()
    }

    override suspend fun connect(): Result<Unit> {
        return kvsWebRtcClient.connectAsMaster()
    }

    override fun disconnect() {
        kvsWebRtcClient.disconnect()
    }

    override fun release() {
        kvsWebRtcClient.release()
    }

    private fun KvsConnectionState.toDomain(): KvsMasterConnectionState {
        return when (this) {
            KvsConnectionState.DISCONNECTED -> KvsMasterConnectionState.DISCONNECTED
            KvsConnectionState.CONNECTING -> KvsMasterConnectionState.CONNECTING
            KvsConnectionState.CONNECTED -> KvsMasterConnectionState.CONNECTED
            KvsConnectionState.ERROR -> KvsMasterConnectionState.ERROR
        }
    }
}
