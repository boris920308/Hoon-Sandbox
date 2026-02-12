package hoon.example.androidsandbox.presentation.kvs.master

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hoon.example.androidsandbox.presentation.kvs.master.component.CameraPreviewPlaceholder
import hoon.example.androidsandbox.presentation.kvs.master.component.ConnectionStatusBadge
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

@Composable
fun KvsMasterScreen(
    viewModel: KvsMasterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KvsMasterScreenContent(
        uiState = uiState,
        onConnectClick = viewModel::connect,
        onDisconnectClick = viewModel::disconnect,
        onToggleCameraClick = viewModel::toggleCamera
    )
}

@Composable
private fun KvsMasterScreenContent(
    uiState: KvsMasterUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onToggleCameraClick: () -> Unit
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
                    text = "Master",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = uiState.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            ConnectionStatusBadge(connectionState = uiState.connectionState)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 중앙: 카메라 프리뷰
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            CameraPreviewPlaceholder(
                cameraFacing = uiState.cameraFacing,
                modifier = Modifier.fillMaxSize()
            )

            // 카메라 전환 버튼 (우측 상단)
            IconButton(
                onClick = onToggleCameraClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "카메라 전환",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 하단: 컨트롤 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (uiState.connectionState == ConnectionState.CONNECTED ||
                uiState.connectionState == ConnectionState.CONNECTING
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
                        imageVector = Icons.Default.Call,
                        contentDescription = "연결 시작",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun KvsMasterScreenPreview() {
    AndroidSandboxTheme {
        KvsMasterScreenContent(
            uiState = KvsMasterUiState(
                channelName = "test-channel",
                connectionState = ConnectionState.DISCONNECTED
            ),
            onConnectClick = {},
            onDisconnectClick = {},
            onToggleCameraClick = {}
        )
    }
}
