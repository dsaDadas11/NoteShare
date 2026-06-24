package com.example.noteshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.noteshare.core.datastore.TokenManager
import com.example.noteshare.core.network.TokenInterceptor
import com.example.noteshare.core.network.UnauthorizedEventBus
import com.example.noteshare.shared.ui.BottomNavBar
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.noteshare.feature.auth.presentation.LoginScreen
import com.example.noteshare.feature.auth.presentation.RegisterScreen
import com.example.noteshare.feature.feed.presentation.FeedListScreen
import com.example.noteshare.feature.feed.presentation.NoteDetailScreen
import com.example.noteshare.feature.feed.presentation.SearchScreen
import com.example.noteshare.feature.notification.presentation.NotificationScreen
import com.example.noteshare.feature.publish.presentation.PublishScreen
import com.example.noteshare.feature.profile.presentation.ProfileScreen
import com.example.noteshare.feature.profile.presentation.EditProfileScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenInterceptor: TokenInterceptor
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var unauthorizedEventBus: UnauthorizedEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册 401 回调：拦截器收到 401 时触发事件总线
        tokenInterceptor.onUnauthorized = { unauthorizedEventBus.emit() }

        setContent {
            com.example.noteshare.core.ui.theme.NoteShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    NoteShareAppScreen()
                }
            }
        }
    }
}

@Composable
fun NoteShareAppScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by mainViewModel.loginState.collectAsState()

    if (isLoggedIn == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    val startDestination = if (isLoggedIn == true) "feed" else "login"

    // 监听 401 事件，自动跳转登录页。必须等 NavHost 创建导航图后再访问 navController.graph。
    LaunchedEffect(Unit) {
        mainViewModel.unauthorizedEvents.collect {
            if (navController.currentDestination == null) {
                return@collect
            }
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            val unreadCount by mainViewModel.unreadCount.collectAsState()
            BottomNavBar(navController = navController, unreadCount = unreadCount)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("feed") {
                val deletedNoteId by navController.currentBackStackEntry!!
                    .savedStateHandle
                    .getStateFlow<Long?>("deletedNoteId", null)
                    .collectAsState()

                LaunchedEffect(deletedNoteId) {
                    if (deletedNoteId != null) {
                        navController.currentBackStackEntry!!
                            .savedStateHandle
                            .remove<Long>("deletedNoteId")
                    }
                }

                FeedListScreen(
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNavigateToProfile = { userId -> navController.navigate("profile/$userId") },
                    refreshSignal = deletedNoteId
                )
            }
            composable("publish") {
                PublishScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onPublishSuccess = {
                        navController.navigate("feed") {
                            popUpTo("feed") { inclusive = true }
                        }
                    }
                )
            }
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        mainViewModel.onLoginSuccess()
                        navController.navigate("feed") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigateUp()
                    }
                )
            }
            composable("note_detail/{noteId}") { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")?.toLongOrNull()
                NoteDetailScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToProfile = { userId -> navController.navigate("profile/$userId") },
                    onDeleteSuccess = {
                        noteId?.let {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("deletedNoteId", it)
                        }
                        navController.navigateUp()
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToEditProfile = { navController.navigate("edit_profile") },
                    onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNavigateToNotification = {
                        mainViewModel.clearUnreadCount()
                        navController.navigate("notification")
                    },
                    onLogout = {
                        mainViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("profile/{userId}") {
                ProfileScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToEditProfile = { },
                    onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNavigateToNotification = { },
                    onLogout = { }
                )
            }
            composable("search") {
                SearchScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNavigateToProfile = { userId -> navController.navigate("profile/$userId") }
                )
            }
            composable("notification") {
                NotificationScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onNavigateToDetail = { noteId -> navController.navigate("note_detail/$noteId") },
                    onNavigateToProfile = { userId -> navController.navigate("profile/$userId") }
                )
            }
            composable("edit_profile") {
                EditProfileScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }
    }
}
