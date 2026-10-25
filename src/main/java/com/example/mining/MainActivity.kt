package com.example.mining

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiningTheme { MiningApp() } }
    }
}

fun NavController.goTab(route: String) = navigate(route) {
    popUpTo("market")
    launchSingleTop = true
}

@Composable
fun MiningApp(vm: MiningViewModel = viewModel()) {
    val nav = rememberNavController()
    val start = remember { if (vm.loggedIn) "market" else "welcome" }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route.orEmpty()
    val showBar = route.isNotEmpty() && route !in setOf("welcome", "consent", "register", "login")

    LaunchedEffect(vm.loggedIn) { if (vm.loggedIn) vm.fetchLocation() }

    val tabs = listOf(
        Triple("market", "Home", Icons.Default.Home),
        Triple("legal", "Legal", Icons.Default.Gavel),
        Triple("map", "Maps", Icons.Default.LocationOn),
        Triple("chats", "Chats", Icons.Default.Chat),
        Triple("profile", "My Profile", Icons.Default.Person)
    )

    Scaffold(bottomBar = {
        if (showBar) NavigationBar {
            tabs.forEach { (r, label, icon) ->
                val selected = when (r) {
                    "market" -> route.startsWith("market") || route.startsWith("detail")
                    "chats" -> route.startsWith("chat")
                    "profile" -> route.startsWith("profile") || route.startsWith("soon")
                    else -> route.startsWith(r)
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { nav.goTab(r) },
                    icon = {
                        BadgedBox(badge = {
                            if (r == "chats" && vm.unread.values.any { it }) Badge()
                        }) { Icon(icon, label) }
                    },
                    label = { Text(label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = start, modifier = Modifier.padding(pad)) {
            composable("welcome") { WelcomeScreen(nav) }
            composable("consent") { ConsentScreen(vm, nav) }
            composable("register") { RegisterScreen(vm, nav) }
            composable("login") { LoginScreen(vm, nav) }
            composable("market") { MarketScreen(vm, nav) }
            composable("detail/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                DetailScreen(it.arguments!!.getInt("id"), vm, nav)
            }
            composable("legal") { SoonScreen("Legal", nav) }
            composable("soon/{title}", listOf(navArgument("title") { type = NavType.StringType })) {
                SoonScreen(it.arguments?.getString("title") ?: "", nav)
            }
            composable("map") { MapScreen(vm) }
            composable("chats") { ChatListScreen(vm, nav) }
            composable("chat/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                ChatScreen(it.arguments!!.getInt("id"), vm, nav)
            }
            composable("profile") { ProfileScreen(vm, nav) }
        }
    }
}