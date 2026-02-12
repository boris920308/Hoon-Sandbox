package hoon.example.androidsandbox.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hoon.example.androidsandbox.navigation.Route
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

data class MenuItem(
    val title: String,
    val route: Route
)

@Composable
fun HomeScreen(
    onNavigate: (Route) -> Unit
) {
    val menuItems = listOf(
        MenuItem(
            title = "CounterScreen",
            route = Route.Counter
        ),
        MenuItem(
            title = "Coming Soon",
            route = Route.Home
        ),
        MenuItem(
            title = "Coming Soon",
            route = Route.Home
        ),
        MenuItem(
            title = "Coming Soon",
            route = Route.Home
        )
    )

    HomeScreenContent(
        menuItems = menuItems,
        onMenuClick = onNavigate
    )
}

@Composable
private fun HomeScreenContent(
    menuItems: List<MenuItem>,
    onMenuClick: (Route) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Android Sandbox",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { item ->
                MenuCard(
                    menuItem = item,
                    onClick = { onMenuClick(item.route) }
                )
            }
        }
    }
}

@Composable
private fun MenuCard(
    menuItem: MenuItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = menuItem.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AndroidSandboxTheme {
        HomeScreenContent(
            menuItems = listOf(
                MenuItem("Counter", Route.Counter),
                MenuItem("Coming Soon", Route.Home)
            ),
            onMenuClick = {}
        )
    }
}
