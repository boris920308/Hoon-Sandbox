package hoon.example.androidsandbox.navigation

sealed class Route(
    val route: String
) {
    data object Home : Route(
        route = "home"
    )

    data object Counter : Route(
        route = "counter"
    )

    data object KvsMaster : Route(
        route = "kvsMaster"
    )

    data object KvsViewer : Route(
        route = "kvsViewer"
    )

}
