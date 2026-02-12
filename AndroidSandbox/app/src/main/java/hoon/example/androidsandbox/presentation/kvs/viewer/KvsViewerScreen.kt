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
        onConnectClick = viewModel::connect,
        onDisconnectClick = viewModel::disconnect
    )
}

@Composable
private fun KvsViewerScreenContent(
    uiState: KvsViewerUiState,
    eglBaseContext: EglBase.Context?,
    onSurfaceReady: (VideoSink) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // 상단: 채널 정보 & 연결 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            ViewerConnectionStatusBadge(connectionState = uiState.connectionState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 중앙: 원격 비디오 영역
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
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VideoPlaceholder(
                            message = "영상 준비 중...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                ViewerConnectionState.CONNECTING -> {
                    VideoPlaceholder(
                        message = "연결 중...",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ViewerConnectionState.DISCONNECTED -> {
                    VideoPlaceholder(
                        message = "연결 버튼을 눌러 시청하세요",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ViewerConnectionState.ERROR -> {
                    VideoPlaceholder(
                        message = "연결 오류 발생",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 하단: 컨트롤 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (uiState.connectionState == ViewerConnectionState.CONNECTED ||
                uiState.connectionState == ViewerConnectionState.CONNECTING
            ) {
                // 연결 종료 버튼
                IconButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Red, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "연결 종료",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                // 연결 시작 버튼
                IconButton(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Green, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "시청 시작",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ViewerConnectionStatusBadge(
    connectionState: ViewerConnectionState
) {
    val (text, color) = when (connectionState) {
        ViewerConnectionState.DISCONNECTED -> "연결 안됨" to Color.Gray
        ViewerConnectionState.CONNECTING -> "연결 중..." to Color.Yellow
        ViewerConnectionState.CONNECTED -> "시청 중" to Color.Green
        ViewerConnectionState.ERROR -> "오류" to Color.Red
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
            onConnectClick = {},
            onDisconnectClick = {}
        )
    }
}
