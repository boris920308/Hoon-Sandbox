package hoon.example.androidsandbox.presentation.kvs.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hoon.example.androidsandbox.presentation.kvs.viewer.component.RemoteVideoView
import hoon.example.androidsandbox.presentation.kvs.viewer.component.VideoPlaceholder
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme
import org.webrtc.EglBase
import org.webrtc.VideoSink

@Composable
fun KvsViewerScreen(
    onBackClick: () -> Unit = {},
    viewModel: KvsViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    KvsViewerScreenContent(
        uiState = uiState,
        eglBaseContext = viewModel.getEglBaseContext(),
        onSurfaceReady = { sink ->
            viewModel.setRemoteVideoSink(sink)
        },
        onSurfaceReleased = viewModel::clearRemoteVideoSink,
        onConnectClick = viewModel::connect,
        onDisconnectClick = viewModel::disconnect,
        onBackClick = onBackClick
    )
}

@Composable
private fun KvsViewerScreenContent(
    uiState: KvsViewerUiState,
    eglBaseContext: EglBase.Context?,
    onSurfaceReady: (VideoSink) -> Unit,
    onSurfaceReleased: () -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "Viewer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = uiState.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            ViewerConnectionStatusBadge(connectionState = uiState.connectionState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            when (uiState.connectionState) {
                ViewerConnectionState.CONNECTED -> {
                    if (eglBaseContext != null) {
                        RemoteVideoView(
                            eglBaseContext = eglBaseContext,
                            onSurfaceReady = onSurfaceReady,
                            onSurfaceReleased = onSurfaceReleased,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VideoPlaceholder(
                            message = "Preparing video...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                ViewerConnectionState.CONNECTING -> {
                    VideoPlaceholder(
                        message = "Connecting...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ViewerConnectionState.DISCONNECTED -> {
                    VideoPlaceholder(
                        message = "Tap connect to start viewing",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ViewerConnectionState.ERROR -> {
                    VideoPlaceholder(
                        message = "Connection error",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (uiState.connectionState == ViewerConnectionState.CONNECTED ||
                uiState.connectionState == ViewerConnectionState.CONNECTING
            ) {
                IconButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(72.dp)
                        .background(Color.Red.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Disconnect",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(72.dp)
                        .background(Color.Green.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerConnectionStatusBadge(
    connectionState: ViewerConnectionState
) {
    val (text, color) = when (connectionState) {
        ViewerConnectionState.DISCONNECTED -> "Disconnected" to Color.Gray
        ViewerConnectionState.CONNECTING -> "Connecting..." to Color.Yellow
        ViewerConnectionState.CONNECTED -> "Streaming" to Color.Green
        ViewerConnectionState.ERROR -> "Error" to Color.Red
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KvsViewerScreenPreview() {
    AndroidSandboxTheme {
        KvsViewerScreenContent(
            uiState = KvsViewerUiState(
                channelName = "test-channel",
                connectionState = ViewerConnectionState.DISCONNECTED
            ),
            eglBaseContext = null,
            onSurfaceReady = {},
            onSurfaceReleased = {},
            onConnectClick = {},
            onDisconnectClick = {},
            onBackClick = {}
        )
    }
}

