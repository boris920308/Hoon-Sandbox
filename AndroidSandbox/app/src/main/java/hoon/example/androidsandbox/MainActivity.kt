package hoon.example.androidsandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSandboxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        CounterScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    // remember 변수 -> viewModel의 데이터 사용
    val count = viewModel.nameList.size

    val backgroundColor = if (count >= 10) {
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
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = { Text("이름을 입력하세요") }
        )

        if (count >= 10) {
            Text(
                text = "목표달성 ! 배경색 변경",
                color = Color.Blue,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.addName()
        }) {
            Text("리스트에 추가")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "등록된 이름 목록", style = MaterialTheme.typography.titleLarge)

        // 2. RecyclerView 대신 사용하는 LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 3. items 함수를 사용해 리스트의 각 요소를 Composable로 변환
            items(viewModel.nameList) { savedName ->
                NameItem(
                    name = savedName,
                    onDeleteClick = { viewModel.removeName(savedName) } // 삭제 로직 전달
                )
            }
        }
    }
}

// 4. 리스트의 한 줄(Item)을 담당하는 UI
@Composable
fun NameItem(name: String, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // 양 끝 배치
        ) {
            Text(
                text = name,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            // 삭제 버튼
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    AndroidSandboxTheme {
        CounterScreen()
    }
}
