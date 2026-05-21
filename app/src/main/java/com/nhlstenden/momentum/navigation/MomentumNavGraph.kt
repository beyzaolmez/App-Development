package com.nhlstenden.momentum.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nhlstenden.momentum.data.repository.PredefinedQuestRepository
import com.nhlstenden.momentum.ui.components.MomentumBottomNav
import com.nhlstenden.momentum.ui.screens.auth.ForgotPasswordScreen
import com.nhlstenden.momentum.ui.screens.auth.SignInScreen
import com.nhlstenden.momentum.ui.screens.auth.SignUpScreen
import com.nhlstenden.momentum.ui.screens.auth.WelcomeScreen
import com.nhlstenden.momentum.ui.screens.friends.FriendsScreen
import com.nhlstenden.momentum.ui.screens.home.HomeScreen
import com.nhlstenden.momentum.ui.screens.profile.ProfileScreen
import com.nhlstenden.momentum.ui.screens.progress.ProgressScreen
import com.nhlstenden.momentum.ui.screens.quest.CompleteScreen
import com.nhlstenden.momentum.ui.screens.quest.QuestDetailScreen
import com.nhlstenden.momentum.ui.screens.reflect.ReflectScreen
import com.nhlstenden.momentum.viewmodel.QuestStateHolder

// Routes where the bottom nav should be visible.
private val mainRoutes = setOf(
    Routes.Home, Routes.Reflect, Routes.Progress, Routes.Friends, Routes.Profile
)

@Composable
fun MomentumApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute in mainRoutes
    val questStateHolder = remember {
        QuestStateHolder(PredefinedQuestRepository())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) MomentumBottomNav(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Welcome,
            modifier = Modifier.padding(padding)
        ) {
            // ---------- Auth flow ----------
            composable(Routes.Welcome) {
                WelcomeScreen(
                    onSignIn = { navController.navigate(Routes.SignIn) },
                    onSignUp = { navController.navigate(Routes.SignUp) }
                )
            }
            composable(Routes.SignIn) {
                SignInScreen(
                    onSignedIn = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Welcome) { inclusive = true }
                        }
                    },
                    onForgot = { navController.navigate(Routes.Forgot) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SignUp) {
                SignUpScreen(
                    onCreated = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Welcome) { inclusive = true }
                        }
                    },
                    onHaveAccount = { navController.navigate(Routes.SignIn) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Forgot) {
                ForgotPasswordScreen(
                    onSent = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------- Main app tabs ----------
            composable(Routes.Home) {
                HomeScreen(
                    quests = questStateHolder.visibleQuests(),
                    isLoading = questStateHolder.isLoading,
                    selectedStatus = questStateHolder.selectedStatus,
                    activeQuestCount = questStateHolder.activeQuestCount(),
                    completedQuestCount = questStateHolder.completedQuestCount(),
                    onStatusSelected = questStateHolder::selectStatus,
                    onStartQuest = questStateHolder::startQuest,
                    onQuestClick = { id -> navController.navigate(Routes.questDetail(id)) }
                )
            }
            composable(Routes.Reflect) { ReflectScreen() }
            composable(Routes.Progress) { ProgressScreen() }
            composable(Routes.Friends) { FriendsScreen() }
            composable(Routes.Profile) {
                ProfileScreen(
                    onSignOut = {
                        navController.navigate(Routes.Welcome) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ---------- Detail screens ----------
            composable(
                route = Routes.QuestDetail,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                QuestDetailScreen(
                    quest = questStateHolder.questById(id),
                    onBack = { navController.popBackStack() },
                    onStart = { questStateHolder.startQuest(id) },
                    onComplete = {
                        questStateHolder.completeQuest(id)
                        navController.navigate(Routes.complete(id))
                    },
                    onSaveForLater = { navController.popBackStack() },
                    onSkip = {
                        questStateHolder.skipQuest(id)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = Routes.Complete,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) {
                CompleteScreen(
                    onReflect = {
                        navController.navigate(Routes.Reflect) {
                            popUpTo(Routes.Home)
                        }
                    },
                    onHome = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
