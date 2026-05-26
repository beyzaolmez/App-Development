package com.nhlstenden.momentum.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    // First-run intro
    const val Onboarding = "onboarding"

    // Auth flow
    const val Welcome = "welcome"
    const val SignIn = "signin"
    const val SignUp = "signup"
    const val Forgot = "forgot"

    // Main app — root of bottom-nav graph
    const val Home = "home"
    const val Reflect = "reflect"
    const val Progress = "progress"
    const val Friends = "friends"
    const val Profile = "profile"

    // Detail screens
    const val QuestDetail = "quest/{id}"
    const val Complete = "complete/{id}"

    fun questDetail(id: String) = "quest/$id"
    fun complete(id: String) = "complete/$id"
}

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val BottomTabs = listOf(
    BottomTab(Routes.Home, "Home", Icons.Outlined.Home),
    BottomTab(Routes.Reflect, "Reflect", Icons.Outlined.SelfImprovement),
    BottomTab(Routes.Progress, "Progress", Icons.Outlined.EmojiEvents),
    BottomTab(Routes.Friends, "Friends", Icons.Outlined.Group),
    BottomTab(Routes.Profile, "Profile", Icons.Outlined.AccountCircle)
)
