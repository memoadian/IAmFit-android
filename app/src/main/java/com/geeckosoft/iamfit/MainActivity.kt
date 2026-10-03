package com.geeckosoft.iamfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.geeckosoft.iamfit.ui.components.FluidBottomBar
import com.geeckosoft.iamfit.ui.components.NavDestination
import com.geeckosoft.iamfit.data.TokenStore
import com.geeckosoft.iamfit.ui.screens.auth.AuthScreen
import com.geeckosoft.iamfit.ui.screens.home.HomeScreen
import com.geeckosoft.iamfit.ui.screens.log.LogScreen
import com.geeckosoft.iamfit.ui.screens.profile.ProfileScreen
import com.geeckosoft.iamfit.ui.screens.progress.ProgressScreen
import com.geeckosoft.iamfit.ui.theme.IamFitTheme

private val destinations = listOf(
    NavDestination("home", "Inicio", Icons.Rounded.Home),
    NavDestination("log", "Registrar", Icons.Rounded.AutoAwesome),
    NavDestination("progress", "Progreso", Icons.AutoMirrored.Rounded.TrendingUp),
    NavDestination("profile", "Perfil", Icons.Rounded.Person),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IamFitTheme {
                IamFitApp()
            }
        }
    }
}

@Composable
fun IamFitApp() {
    val context = LocalContext.current
    var isAuthenticated by remember { mutableStateOf(TokenStore.get(context) != null) }

    // Sin token de Sanctum no hay nada que sincronizar: primero login/registro.
    if (!isAuthenticated) {
        AuthScreen(onAuthenticated = { isAuthenticated = true })
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            FluidBottomBar(
                destinations = destinations,
                selectedRoute = currentRoute,
                onSelect = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            IamFitNavHost(
                navController = navController,
                onLogout = {
                    TokenStore.clear(context)
                    isAuthenticated = false
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun IamFitNavHost(
    navController: NavHostController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(140)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(140)) },
    ) {
        composable("home") { HomeScreen(modifier = Modifier.fillMaxSize()) }
        composable("log") { LogScreen(modifier = Modifier.fillMaxSize()) }
        composable("progress") { ProgressScreen(modifier = Modifier.fillMaxSize()) }
        composable("profile") { ProfileScreen(onLogout = onLogout, modifier = Modifier.fillMaxSize()) }
    }
}
