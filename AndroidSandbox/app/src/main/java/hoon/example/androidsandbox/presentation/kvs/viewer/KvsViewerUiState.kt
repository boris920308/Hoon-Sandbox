package hoon.example.androidsandbox.presentation.kvs.viewer

data class KvsViewerUiState(
    val channelName: String = "",
    val connectionState: ViewerConnectionState = ViewerConnectionState.DISCONNECTED,
    val errorMessage: String? = null
)

enum class ViewerConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
