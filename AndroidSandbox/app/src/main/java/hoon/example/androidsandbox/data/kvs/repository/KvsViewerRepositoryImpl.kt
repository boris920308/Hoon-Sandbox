package hoon.example.androidsandbox.data.kvs.repository

import android.content.Context
import hoon.example.androidsandbox.data.kvs.KvsConnectionState
import hoon.example.androidsandbox.data.kvs.KvsViewerClient
import hoon.example.androidsandbox.domain.kvs.repository.KvsViewerConnectionState
import hoon.example.androidsandbox.domain.kvs.repository.KvsViewerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.webrtc.EglBase
import org.webrtc.VideoSink
import javax.inject.Inject

class KvsViewerRepositoryImpl @Inject constructor(
    private val kvsViewerClient: KvsViewerClient
) : KvsViewerRepository {

    override val connectionState: Flow<KvsViewerConnectionState>
        get() = kvsViewerClient.connectionState.map { it.toDomain() }

    override fun initialize(context: Context) {
        kvsViewerClient.initializePeerConnectionFactory(context)
    }

    override fun getEglBaseContext(): EglBase.Context? {
        return kvsViewerClient.getEglBaseContext()
    }

    override fun setRemoteVideoSink(sink: VideoSink) {
        kvsViewerClient.setRemoteVideoSink(sink)
    }

    override suspend fun connect(): Result<Unit> {
        return kvsViewerClient.connectAsViewer()
    }

    override fun disconnect() {
        kvsViewerClient.disconnect()
    }

    override fun release() {
        kvsViewerClient.release()
    }

    private fun KvsConnectionState.toDomain(): KvsViewerConnectionState {
        return when (this) {
            KvsConnectionState.DISCONNECTED -> KvsViewerConnectionState.DISCONNECTED
            KvsConnectionState.CONNECTING -> KvsViewerConnectionState.CONNECTING
            KvsConnectionState.CONNECTED -> KvsViewerConnectionState.CONNECTED
            KvsConnectionState.ERROR -> KvsViewerConnectionState.ERROR
        }
    }
}
