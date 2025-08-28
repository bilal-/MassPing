package dev.bilalahmad.massping.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import dev.bilalahmad.massping.R
import dev.bilalahmad.massping.ui.screens.ContactsScreen
import dev.bilalahmad.massping.ui.screens.MessagesScreen
import dev.bilalahmad.massping.ui.screens.NewMessageScreen
import dev.bilalahmad.massping.ui.screens.SettingsScreen
import dev.bilalahmad.massping.ui.viewmodels.MainViewModel

private const val TAG = "MassPingApp"

@Composable
fun MassPingApp(viewModel: MainViewModel) {
    Log.d(TAG, "MassPingApp composing")
    val navController = rememberNavController()

    Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Remove default insets
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_contacts), contentDescription = "Contacts") },
                        label = { Text("Contacts") },
                        selected = currentDestination?.hierarchy?.any { it.route == "contacts" } == true,
                        onClick = {
                            navController.navigate("contacts") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_messages), contentDescription = "Messages") },
                        label = { Text("Messages") },
                        selected = currentDestination?.hierarchy?.any { it.route == "messages" } == true,
                        onClick = {
                            navController.navigate("messages") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_new_message), contentDescription = "New Message") },
                        label = { Text("New Message") },
                        selected = currentDestination?.hierarchy?.any { it.route == "new_message" } == true,
                        onClick = {
                            navController.navigate("new_message") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = { Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "contacts",
                modifier = Modifier
                    .padding(innerPadding)
                    .statusBarsPadding()
            ) {
                composable("contacts") {
                    ContactsScreen(
                        viewModel = viewModel,
                        onSendMessage = { selectedContactIds ->
                            val contactIdsParam = selectedContactIds.joinToString(",")
                            navController.navigate("new_message?selectedContacts=$contactIdsParam") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("messages") {
                    MessagesScreen(
                        viewModel = viewModel,
                        onNavigateToNewMessage = {
                            navController.navigate("new_message") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(
                    "new_message?selectedContacts={selectedContacts}",
                    arguments = listOf(navArgument("selectedContacts") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    })
                ) { backStackEntry ->
                    val selectedContactsParam = backStackEntry.arguments?.getString("selectedContacts")
                    val preselectedContactIds = if (!selectedContactsParam.isNullOrEmpty()) {
                        selectedContactsParam.split(",").filter { it.isNotBlank() }
                    } else {
                        emptyList()
                    }

                    NewMessageScreen(
                        viewModel = viewModel,
                        preselectedContactIds = preselectedContactIds,
                        onMessageCreated = {
                            navController.navigate("messages") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
}
