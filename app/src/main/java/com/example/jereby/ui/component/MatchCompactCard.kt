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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.jereby.data.model.Match


@Composable
fun MatchCompactCard(
    match: Match,
    leftName: String,
    rightName: String,
    leftPlayer: String,
    rightPlayer: String,
    leftLogo: String?,
    rightLogo: String?,
    onSubmit: (Int, Int) -> Unit,
    onEdit: (Int, Int) -> Unit = { _, _ -> },
) {
    var h by remember(match.id) { mutableStateOf(match.homeScore?.toString() ?: "") }
    var a by remember(match.id) { mutableStateOf(match.awayScore?.toString() ?: "") }
    var editing by remember(match.id) { mutableStateOf(false) }
    val saved = match.winnerClubId != null && !editing
    val leftIsWinner = saved && match.winnerClubId == match.homeClubId
    val rightIsWinner = saved && match.winnerClubId == match.awayClubId

    val fieldsEnabled = editing || match.winnerClubId == null

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {

            // Header row: icons + two stacked labels; no weight-based spacer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!leftLogo.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(leftLogo)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(
                    Modifier
                        .width(0.dp)
                        .weight(1f)
                ) {
                    Text(
                        leftName,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                leftPlayer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        enabled = false
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    Modifier
                        .width(0.dp)
                        .weight(1f), horizontalAlignment = Alignment.End
                ) {
                    Text(
                        rightName,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                rightPlayer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        enabled = false
                    )
                }
                if (!rightLogo.isNullOrBlank()) {
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(rightLogo)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Scores: fixed min widths to avoid squish
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = h, onValueChange = { h = it },
                    label = { Text(if (leftIsWinner) "Home (W)" else "Home") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(min = 80.dp)
                        .weight(1f),
                    enabled = !saved
                )
                OutlinedTextField(
                    value = a, onValueChange = { a = it },
                    label = { Text(if (rightIsWinner) "Away (W)" else "Away") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .widthIn(min = 80.dp)
                        .weight(1f),
                    enabled = !saved
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val homeInt = h.toIntOrNull()
                val awayInt = a.toIntOrNull()
                val validScores = homeInt != null && awayInt != null && homeInt != awayInt

                Button(
                    enabled = fieldsEnabled && validScores,
                    onClick = {
                        if (!validScores) return@Button // extra guard, shouldn't happen if disabled
                        if (match.winnerClubId == null) onSubmit(homeInt, awayInt)
                        else {
                            onEdit(homeInt, awayInt); editing = false
                        }
                    }
                ) { Text(if (match.winnerClubId == null) "Save" else if (editing) "Apply" else "Save") }

                if (match.winnerClubId != null) {
                    OutlinedButton(onClick = { editing = !editing }) {
                        Text(if (editing) "Cancel" else "Edit")
                    }
                }
            }
        }
    }
}