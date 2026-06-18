package com.nhlstenden.momentum.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector

// Sub-routes within the Friends screen
object FriendsRoutes {
    const val Connections = "friends/connections"
    const val Streaks = "friends/streaks"
    const val FindFriends = "friends/find"
}

data class FriendsTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val FriendsTabs = listOf(
    FriendsTab(FriendsRoutes.Connections, "My Friends", Icons.Outlined.Group),
    FriendsTab(FriendsRoutes.Streaks, "Streaks", Icons.Outlined.LocalFireDepartment),
    FriendsTab(FriendsRoutes.FindFriends, "Find", Icons.Outlined.PersonAdd)
)
