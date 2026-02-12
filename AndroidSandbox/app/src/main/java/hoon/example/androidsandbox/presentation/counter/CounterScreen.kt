package hoon.example.androidsandbox.presentation.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hoon.example.androidsandbox.domain.counter.model.Name
import hoon.example.androidsandbox.presentation.counter.component.NameItem
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CounterScreenContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onAddClick = viewModel::addName,
        onDeleteClick = viewModel::removeName
    )
}

@Composable
private fun CounterScreenContent(
    uiState: CounterUiState,
    onNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Name) -> Unit
) {
    val backgroundColor = if (uiState.isGoalReached) {
        Color(0xFFE1F5FE)
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = uiState.inputName,
            onValueChange = onNameChange,
            label = { Text("이름을 입력하세요") }
        )

        if (uiState.isGoalReached) {
            Text(
                text = "목표달성 ! 배경색 변경",
                color = Color.Blue,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAddClick) {
            Text("리스트에 추가")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "등록된 이름 목록",
            style = MaterialTheme.typography.titleLarge
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.nameList) { name ->
                NameItem(
                    name = name,
                    onDeleteClick = { onDeleteClick(name) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CounterScreenPreview() {
    AndroidSandboxTheme {
        CounterScreenContent(
            uiState = CounterUiState(),
            onNameChange = {},
            onAddClick = {},
            onDeleteClick = {}
        )
    }
}
