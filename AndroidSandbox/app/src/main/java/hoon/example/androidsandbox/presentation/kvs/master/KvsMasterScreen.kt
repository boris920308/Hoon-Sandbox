package hoon.example.androidsandbox.presentation.kvs.master

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hoon.example.androidsandbox.presentation.kvs.master.component.CameraPreview
import hoon.example.androidsandbox.presentation.kvs.master.component.CameraPreviewPlaceholder
import hoon.example.androidsandbox.presentation.kvs.master.component.ConnectionStatusBadge
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

@Composable
fun KvsMasterScreen(
    viewModel: KvsMasterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
        if (hasPermissions) {
            viewModel.initialize(context)
            viewModel.startLocalVideo(context)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    KvsMasterScreenContent(
        uiState = uiState,
        hasPermissions = hasPermissions,
        eglBaseContext = viewModel.getEglBaseContext(),
        onSurfaceReady = { sink ->
            viewModel.setLocalVideoSink(sink)
        },
        onConnectClick = viewModel::connect,
        onDisconnectClick = viewModel::disconnect,
        onToggleCameraClick = viewModel::toggleCamera
    )
}

@Composable
private fun KvsMasterScreenContent(
    uiState: KvsMasterUiState,
    hasPermissions: Boolean,
    eglBaseContext: org.webrtc.EglBase.Context?,
    onSurfaceReady: (org.webrtc.VideoSink) -> Unit,
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

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (hasPermissions && eglBaseContext != null) {
                CameraPreview(
                    eglBaseContext = eglBaseContext,
                    onSurfaceReady = onSurfaceReady,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CameraPreviewPlaceholder(
                    cameraFacing = uiState.cameraFacing,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(
                onClick = onToggleCameraClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Switch camera",
                    tint = Color.White
                )
            }

            if (uiState.connectionState == ConnectionState.CONNECTED ||
                uiState.connectionState == ConnectionState.CONNECTING
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
                        imageVector = Icons.Default.Call,
                        contentDescription = "Connect",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
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
            hasPermissions = false,
            eglBaseContext = null,
            onSurfaceReady = {},
            onConnectClick = {},
            onDisconnectClick = {},
            onToggleCameraClick = {}
        )
    }
}
