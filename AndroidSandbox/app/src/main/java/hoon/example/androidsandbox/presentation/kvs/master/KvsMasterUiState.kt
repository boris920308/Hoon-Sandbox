package hoon.example.androidsandbox.presentation.kvs.master

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class CameraFacing {
    FRONT,
    BACK
}

data class KvsMasterUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val cameraFacing: CameraFacing = CameraFacing.FRONT,
    val errorMessage: String? = null,
    val channelName: String = ""
)
