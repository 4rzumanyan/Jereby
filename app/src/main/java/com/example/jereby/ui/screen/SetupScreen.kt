package com.example.jereby.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.jereby.domain.model.ClubSourceKind
import com.example.jereby.ui.TournamentViewModel
import com.example.jereby.ui.model.SourceUi
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    nav: NavHostController,
    vm: TournamentViewModel = hiltViewModel(),
) {
    var title by remember { mutableStateOf("My Tournament") }
    var clubCountText by remember { mutableStateOf("32") }
    var players by remember { mutableStateOf(listOf("Player 1", "Player 2")) }
    var sourceUi by remember { mutableStateOf(SourceUi.UEFA) }
    var isCreating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Title") })
        OutlinedTextField(
            clubCountText,
            { clubCountText = it },
            label = { Text("Club count (even)") })
        Spacer(Modifier.height(8.dp))
        Text("Club Source", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = sourceUi == SourceUi.UEFA,
                onClick = { sourceUi = SourceUi.UEFA })
            Text("UEFA.com")
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = sourceUi == SourceUi.LOCAL,
                onClick = { sourceUi = SourceUi.LOCAL })
            Text("Local JSON")
        }

        Text("Players", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        players.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        players = players.toMutableList().also { it[index] = newValue }
                    },
                    label = { Text("Player ${index + 1}") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        players = players.toMutableList().also { it.removeAt(index) }
                    },
                    enabled = players.size > 1
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove player")
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        Button(
            onClick = { players = players + "Player ${players.size + 1}" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Player")
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !isCreating,
            onClick = {
                val n = clubCountText.toIntOrNull() ?: 32
                val src =
                    if (sourceUi == SourceUi.UEFA) ClubSourceKind.UEFA else ClubSourceKind.LOCAL
                isCreating = true
                // call suspend and navigate
                coroutineScope.launch {
                    val id = vm.createAndReturnId(title, players.filter { it.isNotBlank() }, n, src)
                    isCreating = false
                    nav.navigate("bracket/$id") { popUpTo("home") }
                }
            }
        ) { Text(if (isCreating) "Creating..." else "Create & Draw") }
    }
}