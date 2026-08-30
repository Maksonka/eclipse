package com.shadowvibe.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shadowvibe.app.ui.auth.AuthScreen
import com.shadowvibe.app.ui.chat.ChatScreen
import com.shadowvibe.app.ui.chatlist.ChatListScreen
import com.shadowvibe.app.ui.group.CreateGroupScreen
import com.shadowvibe.app.ui.group.GroupChatScreen
import com.shadowvibe.app.ui.profile.EditProfileScreen
import com.shadowvibe.app.ui.profile.ProfileScreen
import com.shadowvibe.app.ui.settings.SettingsScreen
import com.shadowvibe.app.ui.watch.WatchHubScreen
import com.shadowvibe.app.ui.watch.WatchRoomScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object ChatList : Screen("chatlist")
    object Chat : Screen("chat/{username}") {
        fun createRoute(username: String): String = "chat/$username"
    }
    object GroupChat : Screen("group/{groupId}") {
        fun createRoute(groupId: Long): String = "group/$groupId"
    }
    object Profile : Screen("profile/{username}") {
        fun createRoute(username: String): String = "profile/$username"
    }
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object CreateGroup : Screen("create_group")
    object WatchHub : Screen("watch_hub")
    object WatchRoom : Screen("watch_room/{roomId}/{roomCode}") {
        fun createRoute(roomId: Long, roomCode: String): String = "watch_room/$roomId/$roomCode"
    }
}

@Composable
fun ShadowVibeNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(navController)
        }
        composable(Screen.ChatList.route) {
            ChatListScreen(navController)
        }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            ChatScreen(navController, username)
        }
        composable(
            route = Screen.GroupChat.route,
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
            GroupChatScreen(navController, groupId)
        }
        composable(Screen.Profile.route,
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            ProfileScreen(navController, username)
        }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(navController)
        }
        composable(Screen.WatchHub.route) {
            WatchHubScreen(navController)
        }
        composable(
            route = Screen.WatchRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.LongType },
                navArgument("roomCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getLong("roomId") ?: 0L
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            WatchRoomScreen(navController, roomId, roomCode)
        }
    }
}
