package com.example.jereby.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jereby.ui.screen.BracketScreen
import com.example.jereby.ui.screen.HomeScreen
import com.example.jereby.ui.screen.SetupScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JerebyAppRoot() {
    val nav = rememberNavController()
    MaterialTheme {
        Scaffold(topBar = { TopAppBar(title = { Text("Jereby") }) }) { inner ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(inner)
            ) {
                composable("home") { HomeScreen(nav) }
                composable("setup") { SetupScreen(nav) }
                composable(
                    route = "bracket/{tid}",
                    arguments = listOf(navArgument("tid") { type = NavType.LongType })
                ) { backStack ->
                    val tid = backStack.arguments!!.getLong("tid")
                    BracketScreen(
                        tournamentIdArg = tid,
                        onNewTournament = { nav.navigate("setup") { popUpTo("home") } }
                    )
                }
            }
        }
    }
}