package com.example.emptyapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.emptyapp.ui.components.MomentumBottomNav
import com.example.emptyapp.ui.screens.auth.ForgotPasswordScreen
import com.example.emptyapp.ui.screens.onboarding.InterestSelectionScreen
import com.example.emptyapp.ui.screens.auth.SignInScreen
import com.example.emptyapp.ui.screens.auth.SignUpScreen
import com.example.emptyapp.ui.screens.auth.WelcomeScreen
import com.example.emptyapp.ui.screens.friends.FriendsScreen
import com.example.emptyapp.ui.screens.home.HomeScreen
import com.example.emptyapp.ui.screens.profile.FeedbackScreen
import com.example.emptyapp.ui.screens.profile.ProfileScreen
import com.example.emptyapp.ui.screens.profile.SuggestQuestScreen
import com.example.emptyapp.ui.screens.progress.ProgressScreen
import com.example.emptyapp.ui.screens.quest.CompleteScreen
import com.example.emptyapp.ui.screens.quest.QuestDetailScreen
import com.example.emptyapp.ui.screens.reflect.ReflectScreen

// Routes where the bottom nav should be visible.
private val mainRoutes = setOf(
    Routes.Home, Routes.Reflect, Routes.Progress, Routes.Friends, Routes.Profile
)

@Composable
fun MomentumApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute in mainRoutes

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
                        // New users pick their interests before landing on Home
                        navController.navigate(Routes.Interests) {
                            popUpTo(Routes.Welcome) { inclusive = true }
                        }
                    },
                    onHaveAccount = { navController.navigate(Routes.SignIn) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Interests) {
                InterestSelectionScreen(
                    onContinue = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Interests) { inclusive = true }
                        }
                    }
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
                    },
                    onSuggestQuest = { navController.navigate(Routes.SuggestQuest) },
                    onFeedback = { navController.navigate(Routes.Feedback) }
                )
            }

            // ---------- Profile sub-screens ----------
            composable(Routes.SuggestQuest) {
                SuggestQuestScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Feedback) {
                FeedbackScreen(onBack = { navController.popBackStack() })
            }

            // ---------- Detail screens ----------
            composable(
                route = Routes.QuestDetail,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                QuestDetailScreen(
                    questId = id,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.navigate(Routes.complete(id)) },
                    onSaveForLater = { navController.popBackStack() },
                    onSkip = { navController.popBackStack() }
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
