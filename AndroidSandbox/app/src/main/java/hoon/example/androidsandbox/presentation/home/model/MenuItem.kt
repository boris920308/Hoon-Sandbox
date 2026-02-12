package hoon.example.androidsandbox.presentation.home.model

import androidx.annotation.StringRes
import hoon.example.androidsandbox.navigation.Route

data class MenuItem(
    @StringRes val titleResId: Int,
    val route: Route
)
