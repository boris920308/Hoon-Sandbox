package hoon.example.androidsandbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import hoon.example.androidsandbox.presentation.counter.CounterScreen
import hoon.example.androidsandbox.presentation.home.HomeScreen
import hoon.example.androidsandbox.presentation.kvs.master.KvsMasterScreen
import hoon.example.androidsandbox.presentation.kvs.viewer.KvsViewerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                onNavigate = { route ->
                    if (route != Route.Home) {
                        navController.navigate(route.route)
                    }
                }
            )
        }

        composable(Route.Counter.route) {
            CounterScreen()
        }

        composable(Route.KvsMaster.route) {
            KvsMasterScreen()
        }

        composable(Route.KvsViewer.route) {
            KvsViewerScreen()
        }
    }
}
