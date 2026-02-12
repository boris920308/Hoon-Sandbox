package hoon.example.androidsandbox.navigation

import androidx.annotation.StringRes
import hoon.example.androidsandbox.R

sealed class Route(
    @StringRes val titleResId: Int,
    val route: String
) {
    data object Home : Route(
        titleResId = R.string.route_home,
        route = "home"
    )

    data object Counter : Route(
        titleResId = R.string.route_counter,
        route = "counter"
    )

    data object KvsMaster : Route(
        titleResId = R.string.route_kvs_master,
        route = "kvsMaster"
    )

    data object KvsViewer : Route(
        titleResId = R.string.route_kvs_viewer,
        route = "kvsViewer"
    )

}
