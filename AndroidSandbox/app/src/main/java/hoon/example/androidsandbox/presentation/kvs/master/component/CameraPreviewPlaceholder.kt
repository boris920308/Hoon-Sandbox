package hoon.example.androidsandbox.presentation.kvs.master.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hoon.example.androidsandbox.presentation.kvs.master.CameraFacing

@Composable
fun CameraPreviewPlaceholder(
    cameraFacing: CameraFacing,
    modifier: Modifier = Modifier
) {
    // TODO: 실제 카메라 프리뷰로 교체
    Box(
        modifier = modifier.background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "카메라 프리뷰",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = if (cameraFacing == CameraFacing.FRONT) "전면 카메라" else "후면 카메라",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
