package com.example.jereby

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jereby.ui.TournamentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JerebyAppRoot() }
    }
}

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

@Composable
fun SetupScreen(
    nav: NavHostController,
    vm: TournamentViewModel = hiltViewModel(),
) {
    var title by remember { mutableStateOf("My Tournament") }
    var clubCountText by remember { mutableStateOf("32") }
    var players by remember { mutableStateOf(listOf("Player 1", "Player 2")) }

    Surface {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") })
            OutlinedTextField(
                clubCountText, { clubCountText = it },
                label = { Text("Club count (even)") }
            )
            Text("Players")
            players.forEachIndexed { i, name ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { new ->
                        players = players.toMutableList().also { it[i] = new }
                    },
                    label = { Text("Player ${i + 1}") },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                OutlinedButton(onClick = {
                    players = players + "Player ${players.size + 1}"
                }) { Text("Add player") }
                SpacerW(8)
                if (players.size > 2) {
                    OutlinedButton(onClick = { players = players.dropLast(1) }) { Text("Remove") }
                }
            }
            SpacerW(16)
            Button(onClick = {
                val n = clubCountText.toIntOrNull() ?: 32
                // launch because create returns id
                CoroutineScope(Dispatchers.Main).launch {
                    val id = vm.createAndReturnId(title, players.filter { it.isNotBlank() }, n)
                    nav.navigate("bracket/$id") {
                        popUpTo("setup") { inclusive = true } // avoid back to setup
                    }
                }
            }) { Text("Create & Draw") }
        }
    }
}

@Composable
private fun SpacerW(dp: Int) = Spacer(modifier = Modifier.width(dp.dp))

@Composable
fun BracketScreen(
    tournamentIdArg: Long,
    vm: TournamentViewModel = hiltViewModel(),
    onNewTournament: () -> Unit = {}, // optional callback to navigate back to setup
) {
    LaunchedEffect(tournamentIdArg) { vm.loadExisting(tournamentIdArg) }

    val matches by vm.currentMatches.collectAsState()
    val clubs by vm.clubsMap.collectAsState()
    val players by vm.playersMap.collectAsState()
    val roundTitle by vm.currentRoundTitle.collectAsState()
    val champion by vm.championName.collectAsState()

    Column(Modifier.padding(16.dp)) {
        if (champion != null) {
            // Winner banner
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Champion", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(champion!!, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onNewTournament) { Text("New Tournament") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(roundTitle ?: "Current Round", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (matches.isEmpty()) {
            Text("No matches yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(matches) { match ->
                    val homeClub = clubs[match.homeClubId]
                    val awayClub = clubs[match.awayClubId]
                    val homePlayer =
                        players[match.homePlayerId]?.displayName ?: "P${match.homePlayerId}"
                    val awayPlayer =
                        players[match.awayPlayerId]?.displayName ?: "P${match.awayPlayerId}"

                    MatchCard(
                        leftName = homeClub?.name ?: match.homeClubId,
                        rightName = awayClub?.name ?: match.awayClubId,
                        leftPlayer = homePlayer,
                        rightPlayer = awayPlayer,
                        leftLogo = homeClub?.logoUrl,
                        rightLogo = awayClub?.logoUrl,
                        match = match,
                        onSubmit = { h, a -> vm.submitScore(match.id, h, a) }
                    )
                }
            }
        }

        val pStats by vm.playerStats.collectAsState()
        Spacer(Modifier.height(16.dp))
        Text("Player Stats", style = MaterialTheme.typography.titleMedium)
        pStats.forEach { s ->
            val name = vm.playersMap.value[s.playerId]?.displayName ?: "Player ${s.playerId}"
            Text("$name — W:${s.wins} L:${s.losses} GF:${s.goalsFor} GA:${s.goalsAgainst}")
        }
    }
}


@Composable
fun MatchCard(
    leftName: String,
    rightName: String,
    leftPlayer: String,
    rightPlayer: String,
    leftLogo: String?,
    rightLogo: String?,
    match: com.example.jereby.data.Match,
    onSubmit: (Int, Int) -> Unit,
) {
    var homeScore by remember(match.id) { mutableStateOf(match.homeScore?.toString() ?: "") }
    var awayScore by remember(match.id) { mutableStateOf(match.awayScore?.toString() ?: "") }
    val saved = match.winnerClubId != null

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!leftLogo.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = leftLogo,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column {
                    Text(leftName, style = MaterialTheme.typography.titleSmall)
                    AssistChip(onClick = {}, label = { Text(leftPlayer) }, enabled = false)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(rightName, style = MaterialTheme.typography.titleSmall)
                    AssistChip(onClick = {}, label = { Text(rightPlayer) }, enabled = false)
                }
                Spacer(Modifier.width(8.dp))
                if (!rightLogo.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = rightLogo,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = homeScore, onValueChange = { homeScore = it },
                    label = { Text(leftName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), enabled = !saved
                )
                OutlinedTextField(
                    value = awayScore, onValueChange = { awayScore = it },
                    label = { Text(rightName) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f), enabled = !saved
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !saved && homeScore.toIntOrNull() != null && awayScore.toIntOrNull() != null,
                onClick = { onSubmit(homeScore.toInt(), awayScore.toInt()) }
            ) { Text(if (saved) "Saved" else "Save Result") }
        }
    }
}

@Composable
fun HomeScreen(
    nav: NavHostController,
    vm: TournamentViewModel = hiltViewModel(),
) {
    val ts by vm.allTournaments.collectAsState()
    Column(Modifier.padding(16.dp)) {
        Button(onClick = { nav.navigate("setup") }) { Text("New Tournament") }
        Spacer(Modifier.height(12.dp))
        if (ts.isEmpty()) {
            Text("No tournaments yet.")
        } else {
            Text("Tournaments", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ts.forEach { t ->
                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { nav.navigate("bracket/${t.id}") }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(t.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Created: ${
                                java.text.SimpleDateFormat("yyyy-MM-dd")
                                    .format(java.util.Date(t.createdAt))
                            }"
                        )
                        Text("Status: ${t.status}")
                    }
                }
            }
        }
    }
}



