package hoon.example.androidsandbox.presentation.kvs.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

@Composable
fun KvsViewerScreen() {
    KvsViewerScreenContent()
}

@Composable
private fun KvsViewerScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KvsViewerScreen",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black

        )
    }
}

@Preview(showBackground = true)
@Composable
private fun KvsViewerScreenPreview() {
    AndroidSandboxTheme() {
        KvsViewerScreen()
    }
}