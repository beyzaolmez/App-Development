package com.nhlstenden.momentum.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nhlstenden.momentum.data.repository.AuthRepository
import com.nhlstenden.momentum.ui.components.MomentumBottomNav
import com.nhlstenden.momentum.ui.screens.auth.ForgotPasswordScreen
import com.nhlstenden.momentum.ui.screens.auth.SignInScreen
import com.nhlstenden.momentum.ui.screens.auth.SignUpScreen
import com.nhlstenden.momentum.ui.screens.auth.WelcomeScreen
import com.nhlstenden.momentum.ui.screens.friends.FriendsScreen
import com.nhlstenden.momentum.ui.screens.home.HomeScreen
import com.nhlstenden.momentum.ui.screens.home.Quest
import com.nhlstenden.momentum.ui.screens.home.QuestStatus
import com.nhlstenden.momentum.ui.screens.home.sampleQuests
import com.nhlstenden.momentum.ui.screens.onboarding.InterestSelectionScreen
import com.nhlstenden.momentum.ui.screens.onboarding.OnboardingScreen
import com.nhlstenden.momentum.ui.screens.profile.ProfileScreen
import com.nhlstenden.momentum.ui.screens.progress.ProgressScreen
import com.nhlstenden.momentum.ui.screens.quest.CompleteScreen
import com.nhlstenden.momentum.ui.screens.quest.QuestDetailScreen
import com.nhlstenden.momentum.ui.screens.reflect.ReflectScreen

// Routes where the bottom nav should be visible.
private val mainRoutes = setOf(
    Routes.Home, Routes.Reflect, Routes.Progress, Routes.Friends, Routes.Profile
)

@Composable
fun MomentumApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute in mainRoutes

    val authRepository = remember { AuthRepository() }
    val currentUser = remember { authRepository.getCurrentUser() }
    val displayName = currentUser?.displayName.orEmpty()
    val email = currentUser?.email.orEmpty()
    val startDestination = if (currentUser != null) Routes.Home else Routes.Onboarding

    // In-memory quest store. Replace with a ViewModel/repository when persistence lands.
    val quests = remember { mutableStateListOf<Quest>().apply { addAll(sampleQuests) } }
    val updateStatus: (String, QuestStatus) -> Unit = { id, status ->
        val i = quests.indexOfFirst { it.id == id }
        if (i >= 0) quests[i] = quests[i].copy(status = status)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) MomentumBottomNav(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            // ---------- First-run intro ----------
            composable(Routes.Onboarding) {
                val toWelcome: () -> Unit = {
                    navController.navigate(Routes.Welcome) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
                OnboardingScreen(
                    onFinish = toWelcome,
                    onSkip = toWelcome
                )
            }

            // ---------- Auth flow ----------
            composable(Routes.Welcome) {
                WelcomeScreen(
                    onSignIn = { navController.navigate(Routes.SignIn) },
                    onSignUp = { navController.navigate(Routes.SignUp) },
                    onPreviewOnboarding = { navController.navigate(Routes.Onboarding) }
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
                    displayName = displayName,
                    quests = quests,
                    onQuestClick = { id -> navController.navigate(Routes.questDetail(id)) }
                )
            }
            composable(Routes.Reflect) { ReflectScreen() }
            composable(Routes.Progress) { ProgressScreen() }
            composable(Routes.Friends) { FriendsScreen() }
            composable(Routes.Profile) {
                ProfileScreen(
                    displayName = displayName,
                    email = email,
                    onSignOut = {
                        authRepository.signOut()
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
                    questId = id,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.navigate(Routes.complete(id)) },
                    onSaveForLater = {
                        updateStatus(id, QuestStatus.SavedForLater)
                        navController.popBackStack()
                    },
                    onSkip = {
                        updateStatus(id, QuestStatus.Skipped)
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
