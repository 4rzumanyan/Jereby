package com.example.jereby.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.jereby.ui.TournamentViewModel
import com.example.jereby.ui.component.ChampionBanner
import com.example.jereby.ui.component.RoundColumnFixed

@Composable
fun BracketScreen(
    tournamentIdArg: Long,
    vm: TournamentViewModel = hiltViewModel(),
    onNewTournament: () -> Unit = {},
) {
    LaunchedEffect(tournamentIdArg) { vm.loadExisting(tournamentIdArg) }

    val clubs by vm.clubsMap.collectAsState()
    val players by vm.playersMap.collectAsState()
    val champion by vm.championName.collectAsState()
    val rwm by vm.roundsWithMatches.collectAsState()

    // Fill the whole screen so children get sane constraints
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        if (champion != null) {
            ChampionBanner(name = champion!!, onNewTournament = onNewTournament)
            Spacer(Modifier.height(16.dp))
        }

        if (rwm.isEmpty()) {
            Text("No matches yet.")
            return@Column
        }

        val columnWidth = 320.dp  // <- key: a sensible fixed width per round column

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(rwm) { (round, matches) ->
                RoundColumnFixed(
                    title = round.displayName,
                    matches = matches,
                    clubsMap = clubs,
                    playersMap = players,
                    columnWidth = columnWidth,
                    onSubmit = { matchId, h, a -> vm.submitScore(matchId, h, a) },
                    onEdit = { matchId, h, a -> vm.editScore(matchId, h, a) }
                )
            }
        }
    }
}