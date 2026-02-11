package hoon.example.androidsandbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun CounterScreen() {
    // 값이 변하면 UI가 자동으로 업데이트 되도록 '상태'로 선언
    var count by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }

    // 1. 이름 리스트를 저장할 상태 (가변 리스트)
    // remember { mutableStateListOf<String>() }는 리스트 내부 요소가 추가/삭제될 때 UI를 갱신해줍니다.
    val nameList = remember { mutableStateListOf<String>() }

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
            value = name,
            onValueChange = { name = it },
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
            if (name.isNotBlank()) {
                nameList.add(name) // 리스트에 이름 추가
                count++            // 카운트 증가
                name = ""          // 입력창 비우기
            }
        }) {
            Text("리스트에 추가 및 더하기")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "등록된 이름 목록", style = MaterialTheme.typography.titleLarge)

        // 2. RecyclerView 대신 사용하는 LazyColumn
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // 3. items 함수를 사용해 리스트의 각 요소를 Composable로 변환
            items(nameList) { savedName ->
                NameItem(savedName)
            }
        }
    }
}

// 4. 리스트의 한 줄(Item)을 담당하는 UI
@Composable
fun NameItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CounterScreenPreview() {
    AndroidSandboxTheme {
        CounterScreen()
    }
}
