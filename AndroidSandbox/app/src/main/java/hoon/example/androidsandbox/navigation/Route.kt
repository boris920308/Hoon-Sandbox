package hoon.example.androidsandbox.navigation

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Counter : Route("counter")
}
