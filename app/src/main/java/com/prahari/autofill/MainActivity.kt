package com.prahari.autofill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prahari.autofill.ui.navigation.FillSession
import com.prahari.autofill.ui.navigation.Routes
import com.prahari.autofill.ui.screens.*
import com.prahari.autofill.ui.theme.PrahariTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrahariTheme {
                PrahariNavHost()
            }
        }
    }
}

@Composable
fun PrahariNavHost() {
    val navController = rememberNavController()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as PrahariApp

    // Start destination: login hai to seedha connect/main
    var startDest by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        startDest = when {
            app.firebase.currentUid == null -> Routes.LOGIN
            !app.secureStorage.isPrahariLoggedIn() -> Routes.PRAHARI_CONNECT
            else -> Routes.MAIN
        }
    }

    if (startDest == null) {
        Surface { /* splash */ }
        return
    }

    NavHost(navController = navController, startDestination = startDest!!) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    if (app.secureStorage.isPrahariLoggedIn())
                        navController.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    else
                        navController.navigate(Routes.PRAHARI_CONNECT) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onGoRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.PRAHARI_CONNECT) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PRAHARI_CONNECT) {
            PrahariConnectScreen(
                onConnected = {
                    navController.navigate(Routes.MAIN) { popUpTo(Routes.PRAHARI_CONNECT) { inclusive = true } }
                }
            )
        }

        // ---- Main tabs ----
        composable(Routes.MAIN) {
            MainTabs(
                onAuditSelected = { auditId, branchId ->
                    navController.navigate(Routes.centerList(auditId, branchId))
                }
            )
        }

        // ---- Fill flow ----
        composable(
            Routes.CENTER_LIST,
            arguments = listOf(
                navArgument("auditId") { type = NavType.StringType },
                navArgument("branchId") { type = NavType.StringType }
            )
        ) { backStack ->
            val auditId = backStack.arguments?.getString("auditId") ?: ""
            val branchId = backStack.arguments?.getString("branchId") ?: ""
            CenterListScreen(auditId, branchId) { centerId ->
                navController.navigate(Routes.cdsPhoto(auditId, branchId, centerId))
            }
        }
        composable(
            Routes.CDS_PHOTO,
            arguments = listOf(
                navArgument("auditId") { type = NavType.StringType },
                navArgument("branchId") { type = NavType.StringType },
                navArgument("centerId") { type = NavType.StringType }
            )
        ) { backStack ->
            val auditId = backStack.arguments?.getString("auditId") ?: ""
            val branchId = backStack.arguments?.getString("branchId") ?: ""
            val centerId = backStack.arguments?.getString("centerId") ?: ""
            CDSPhotoScreen {
                navController.navigate(Routes.cdsResults(auditId, branchId, centerId))
            }
        }
        composable(
            Routes.CDS_RESULTS,
            arguments = listOf(
                navArgument("auditId") { type = NavType.StringType },
                navArgument("branchId") { type = NavType.StringType },
                navArgument("centerId") { type = NavType.StringType }
            )
        ) { backStack ->
            val auditId = backStack.arguments?.getString("auditId") ?: ""
            val branchId = backStack.arguments?.getString("branchId") ?: ""
            val centerId = backStack.arguments?.getString("centerId") ?: ""
            CDSResultsScreen {
                navController.navigate(Routes.autoFill(auditId, branchId, centerId))
            }
        }
        composable(
            Routes.AUTO_FILL,
            arguments = listOf(
                navArgument("auditId") { type = NavType.StringType },
                navArgument("branchId") { type = NavType.StringType },
                navArgument("centerId") { type = NavType.StringType }
            )
        ) {
            var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
            if (result == null) {
                AutoFillScreen { success, msg -> result = success to msg }
            } else {
                val (success, msg) = result!!
                ConfirmationScreen(
                    success = success,
                    message = msg,
                    onFillNext = {
                        FillSession.reset()
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    },
                    onGoHome = {
                        FillSession.resetAll()
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainTabs(onAuditSelected: (String, String) -> Unit) {
    val tabNav = rememberNavController()
    val tabs = listOf(
        Triple(Routes.TAB_FILL, "📝 Fill", "Fill"),
        Triple(Routes.TAB_CHAT, "💬 Chat", "Chat"),
        Triple(Routes.TAB_RULES, "📋 Rules", "Rules")
    )
    var selected by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (route, icon, label) ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = {
                            selected = i
                            tabNav.navigate(route) {
                                popUpTo(tabNav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(icon) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = tabNav,
            startDestination = Routes.TAB_FILL,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TAB_FILL) {
                AuditListScreen(onAuditSelected = onAuditSelected)
            }
            composable(Routes.TAB_CHAT) { ChatScreen() }
            composable(Routes.TAB_RULES) { MyRulesScreen() }
        }
    }
}
