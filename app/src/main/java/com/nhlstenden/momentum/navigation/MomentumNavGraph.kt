package com.nhlstenden.momentum.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.nhlstenden.momentum.data.InterestsStore
import com.nhlstenden.momentum.data.model.QuestFeedbackType
import com.nhlstenden.momentum.notification.NotificationHelper
import com.nhlstenden.momentum.ui.components.MomentumBottomNav
import com.nhlstenden.momentum.ui.screens.auth.ForgotPasswordScreen
import com.nhlstenden.momentum.ui.screens.auth.SignInScreen
import com.nhlstenden.momentum.ui.screens.auth.SignUpScreen
import com.nhlstenden.momentum.ui.screens.auth.WelcomeScreen
import com.nhlstenden.momentum.ui.screens.friends.FriendsScreen
import com.nhlstenden.momentum.ui.screens.home.HomeScreen
import com.nhlstenden.momentum.ui.screens.onboarding.InterestSelectionScreen
import com.nhlstenden.momentum.ui.screens.onboarding.OnboardingScreen
import com.nhlstenden.momentum.ui.screens.profile.ProfileScreen
import com.nhlstenden.momentum.ui.screens.progress.ProgressScreen
import com.nhlstenden.momentum.ui.screens.quest.CompleteScreen
import com.nhlstenden.momentum.ui.screens.quest.QuestDetailScreen
import com.nhlstenden.momentum.ui.screens.quest.QuestReflectionScreen
import com.nhlstenden.momentum.ui.screens.reflect.ReflectScreen
import com.nhlstenden.momentum.viewmodel.ProfileViewModel
import com.nhlstenden.momentum.viewmodel.QuestViewModel

// Routes where the bottom nav should be visible.
private val mainRoutes = setOf(
    Routes.Home, Routes.Reflect, Routes.Progress, Routes.Profile
)

@Composable
fun MomentumApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomNav = currentRoute in mainRoutes
    val context = LocalContext.current
    val questViewModel: QuestViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    LaunchedEffect(questViewModel) {
        questViewModel.attachLocalCache(context.applicationContext)
    }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserId = currentUser?.uid
    val startDestination = when {
        currentUser == null -> Routes.Welcome
        !InterestsStore.hasInterests(context, currentUserId) -> Routes.Onboarding
        else -> Routes.Home
    }
    val navigateInterests: () -> Unit = {
        navController.navigate(Routes.Interests)
    }
    val navigateHome: () -> Unit = {
        questViewModel.refresh()
        navController.navigate(Routes.Home) {
            popUpTo(Routes.Welcome) { inclusive = true }
        }
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
            // ---------- Auth flow ----------
            composable(Routes.Welcome) {
                WelcomeScreen(
                    onSignIn = { navController.navigate(Routes.SignIn) },
                    onSignUp = { navController.navigate(Routes.SignUp) },
                    onContinueWithoutAccount = navigateHome
                )
            }
            composable(Routes.SignIn) {
                SignInScreen(
                    onSignedIn = navigateHome,
                    onForgot = { navController.navigate(Routes.Forgot) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SignUp) {
                SignUpScreen(
                    onCreated = {
                        InterestsStore.clear(context, FirebaseAuth.getInstance().currentUser?.uid)
                        navController.navigate(Routes.Onboarding)
                    },
                    onHaveAccount = { navController.navigate(Routes.SignIn) },
                    onSkipRegistration = navigateHome,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Forgot) {
                ForgotPasswordScreen(
                    onSent = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.Onboarding) {
                OnboardingScreen(
                    onFinish = navigateInterests,
                    onSkip = navigateInterests
                )
            }
            composable(Routes.Interests) {
                InterestSelectionScreen(
                    userId = FirebaseAuth.getInstance().currentUser?.uid,
                    onContinue = navigateHome
                )
            }

            // ---------- Main app tabs ----------
            composable(Routes.Home) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val selectedInterests = InterestsStore.load(context, firebaseUser?.uid)
                HomeScreen(
                    quests = questViewModel.visibleQuests(selectedInterests),
                    isLoading = questViewModel.isLoading,
                    greetingName = firebaseUser?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: firebaseUser?.email?.substringBefore("@")
                        ?: "Testing user",
                    dataMode = questViewModel.dataMode,
                    errorMessage = questViewModel.errorMessage,
                    selectedStatus = questViewModel.selectedStatus,
                    activeQuestCount = questViewModel.activeQuestCount(),
                    completedQuestCount = questViewModel.completedDailyQuestCount(),
                    dailyQuestLimit = questViewModel.dailyQuestLimit(),
                    onStatusSelected = questViewModel::selectStatus,
                    onStartQuest = questViewModel::startQuest,
                    onSkipQuest = questViewModel::skipQuest,
                    onQuestClick = { id -> navController.navigate(Routes.questDetail(id)) }
                )
            }
            composable(Routes.Reflect) {
                ReflectScreen(
                    reflections = questViewModel.recentReflections,
                    questTitleForReflection = questViewModel::questTitleForReflection
                )
            }
            composable(Routes.Progress) {
                ProgressScreen(overview = questViewModel.progressOverview())
            }
            composable(Routes.Friends) {
                FriendsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.Profile) {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                ProfileScreen(
                    displayName = profileViewModel.displayName
                        .ifBlank {
                            firebaseUser?.email?.substringBefore("@") ?: "Testing user"
                        },
                    subtitle = firebaseUser?.email ?: "Demo mode",
                    isDemoUser = firebaseUser == null,
                    selectedInterests = InterestsStore.load(context, firebaseUser?.uid).toList(),
                    isSavingName = profileViewModel.isSaving,
                    saveNameError = profileViewModel.saveError,
                    onSaveDisplayName = { newName -> profileViewModel.updateDisplayName(newName) },
                    onOpenFriends = { navController.navigate(Routes.Friends) },
                    onEditInterests = { navController.navigate(Routes.Interests) },
                    onSendTestNotification = {
                        NotificationHelper.showQuestNotification(
                            context,
                            "Try one small side quest today."
                        )
                    },
                    onSignOut = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Routes.Welcome) {
                            popUpTo(navController.graph.id) { inclusive = true }
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
                    quest = questViewModel.questById(id),
                    onBack = { navController.popBackStack() },
                    onStart = { questViewModel.startQuest(id) },
                    onComplete = {
                        questViewModel.completeQuest(id)
                        navController.navigate(Routes.complete(id))
                    },
                    onSaveForLater = { navController.popBackStack() },
                    onSkip = {
                        questViewModel.skipQuest(id)
                        navController.popBackStack()
                    },
                    selectedFeedback = questViewModel.feedbackForQuest(id),
                    onFeedbackSelected = { feedbackType ->
                        when (feedbackType) {
                            QuestFeedbackType.Like -> questViewModel.likeQuest(id)
                            QuestFeedbackType.Dislike -> questViewModel.dislikeQuest(id)
                            else -> questViewModel.saveFeedback(id, feedbackType)
                        }
                    }
                )
            }
            composable(
                route = Routes.QuestReflection,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                QuestReflectionScreen(
                    quest = questViewModel.questById(id),
                    prompt = questViewModel.reflectionPromptForQuest(id),
                    errorText = questViewModel.journalErrorByQuestId[id],
                    onBack = { navController.popBackStack() },
                    onClose = {
                        val returnedHome = navController.popBackStack(Routes.Home, inclusive = false)
                        if (!returnedHome) {
                            navController.navigate(Routes.Home) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    },
                    onSave = { note, promptChoice, quickTake ->
                        val completed = questViewModel.completeQuestWithReflection(
                            id = id,
                            note = note,
                            promptChoice = promptChoice,
                            quickTake = quickTake
                        )
                        if (completed) {
                            navController.navigate(Routes.complete(id))
                        }
                        completed
                    }
                )
            }
            composable(
                route = Routes.Complete,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: ""
                CompleteScreen(
                    hasReflection = questViewModel.hasReflectionForQuest(id),
                    onReflect = {
                        navController.navigate(Routes.questReflection(id))
                    },
                    onHome = {
                        val returnedHome = navController.popBackStack(Routes.Home, inclusive = false)
                        if (!returnedHome) {
                            navController.navigate(Routes.Home) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
