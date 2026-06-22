package com.nhlstenden.momentum

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nhlstenden.momentum.data.repository.FirestoreUserRepository
import com.nhlstenden.momentum.navigation.MomentumApp
import com.nhlstenden.momentum.ui.theme.MomentumAppTheme
import com.nhlstenden.momentum.ui.theme.MomentumTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            val firebaseAuth = remember { FirebaseAuth.getInstance() }
            val userRepository = remember { FirestoreUserRepository() }
            val scope = rememberCoroutineScope()
            var selectedTheme by remember { mutableStateOf(MomentumAppTheme.Default) }

            DisposableEffect(firebaseAuth, userRepository) {
                fun applyThemeForUser(uid: String?) {
                    if (uid == null) {
                        selectedTheme = MomentumAppTheme.Default
                        return
                    }

                    scope.launch {
                        val storedTheme = runCatching {
                            userRepository.getUser(uid)?.themePreference
                        }.getOrNull()
                        selectedTheme = MomentumAppTheme.fromStorageValue(storedTheme)
                    }
                }

                val listener = FirebaseAuth.AuthStateListener { auth ->
                    applyThemeForUser(auth.currentUser?.uid)
                }
                applyThemeForUser(firebaseAuth.currentUser?.uid)
                firebaseAuth.addAuthStateListener(listener)

                onDispose {
                    firebaseAuth.removeAuthStateListener(listener)
                }
            }

            MomentumTheme(appTheme = selectedTheme) {
                MomentumApp(
                    selectedTheme = selectedTheme,
                    onThemeSelected = { theme ->
                        selectedTheme = theme
                        firebaseAuth.currentUser?.uid?.let { uid ->
                            scope.launch {
                                runCatching {
                                    userRepository.updateThemePreference(uid, theme.storageValue)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
