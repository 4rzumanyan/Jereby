package com.example.jereby.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.jereby.data.model.Match


@Composable
fun MatchCard(
    leftName: String,
    rightName: String,
    leftPlayer: String,
    rightPlayer: String,
    leftLogo: String?,
    rightLogo: String?,
    match: Match,
    onSubmit: (Int, Int) -> Unit,
) {
    var homeScore by remember(match.id) { mutableStateOf(match.homeScore?.toString() ?: "") }
    var awayScore by remember(match.id) { mutableStateOf(match.awayScore?.toString() ?: "") }
    val saved = match.winnerClubId != null

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!leftLogo.isNullOrBlank()) {
                    AsyncImage(
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
                    AsyncImage(
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