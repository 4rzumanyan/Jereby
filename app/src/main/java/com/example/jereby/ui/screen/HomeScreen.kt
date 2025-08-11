package com.example.jereby.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.jereby.ui.TournamentViewModel
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun HomeScreen(
    nav: NavHostController,
    vm: TournamentViewModel = hiltViewModel(),
) {
    val tournaments by vm.allTournaments.collectAsState()
    var toDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .navigationBarsPadding() // avoid bottom cut-off on gesture nav
    ) {
        Button(onClick = { nav.navigate("setup") }) { Text("New Tournament") }
        Spacer(Modifier.height(12.dp))

        if (tournaments.isEmpty()) {
            Text("No tournaments yet.")
        } else {
            Text("Tournaments", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // take remaining height
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp) // padding instead of trailing Spacer
            ) {
                items(items = tournaments, key = { it.id }) { t ->
                    ElevatedCard(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("bracket/${t.id}") }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { toDeleteId = t.id }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                            Text("Created: ${SimpleDateFormat("yyyy-MM-dd").format(Date(t.createdAt))}")
                            Text("Status: ${t.status}")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Single dialog outside the list
    val selected = tournaments.firstOrNull { it.id == toDeleteId }
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { toDeleteId = null },
            title = { Text("Delete tournament?") },
            text = { Text("This will remove all rounds, matches and stats for “${selected.title}”.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = selected.id
                    toDeleteId = null
                    vm.deleteTournament(id)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDeleteId = null }) { Text("Cancel") } }
        )
    }
}
