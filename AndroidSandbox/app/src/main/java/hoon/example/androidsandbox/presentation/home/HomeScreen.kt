package hoon.example.androidsandbox.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import hoon.example.androidsandbox.R
import hoon.example.androidsandbox.navigation.Route
import hoon.example.androidsandbox.presentation.home.component.MenuCard
import hoon.example.androidsandbox.presentation.home.model.MenuItem
import hoon.example.androidsandbox.ui.theme.AndroidSandboxTheme

@Composable
fun HomeScreen(
    onNavigate: (Route) -> Unit
) {
    val menuItems = listOf(
        MenuItem(
            titleResId = Route.Counter.titleResId,
            route = Route.Counter
        ),
        MenuItem(
            titleResId = Route.KvsMaster.titleResId,
            route = Route.KvsMaster
        ),
        MenuItem(
            titleResId = R.string.route_coming_soon,
            route = Route.Home
        ),
        MenuItem(
            titleResId = R.string.route_coming_soon,
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
            text = stringResource(R.string.app_name),
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AndroidSandboxTheme {
        HomeScreenContent(
            menuItems = listOf(
                MenuItem(R.string.route_counter, Route.Counter),
                MenuItem(R.string.route_coming_soon, Route.Home)
            ),
            onMenuClick = {}
        )
    }
}
