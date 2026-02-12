package hoon.example.androidsandbox.presentation.kvs.master.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hoon.example.androidsandbox.presentation.kvs.master.ConnectionState

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (connectionState) {
        ConnectionState.DISCONNECTED -> Color.Gray to "연결 안됨"
        ConnectionState.CONNECTING -> Color.Yellow to "연결 중..."
        ConnectionState.CONNECTED -> Color.Green to "연결됨"
        ConnectionState.ERROR -> Color.Red to "오류"
    }

    Row(
        modifier = modifier
            .background(backgroundColor.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (connectionState == ConnectionState.CONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(12.dp)
                    .padding(end = 4.dp),
                strokeWidth = 2.dp,
                color = Color.Black
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black
        )
    }
}
