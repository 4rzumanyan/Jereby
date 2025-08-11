package com.example.jereby.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jereby.data.model.Club
import com.example.jereby.data.model.Match
import com.example.jereby.data.model.Player


@Composable
fun RoundColumnFixed(
    title: String,
    matches: List<Match>,
    clubsMap: Map<String, Club>,
    playersMap: Map<Long, Player>,
    columnWidth: Dp,
    onSubmit: (Long, Int, Int) -> Unit,
    onEdit: (Long, Int, Int) -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        Modifier
            .width(columnWidth)              // <- fixed width per column
            .fillMaxHeight()
            .padding(4.dp)
            .verticalScroll(scroll)          // <- no nested LazyColumn
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))

        matches.sortedBy { it.position }.forEach { m ->
            val homeClub = clubsMap[m.homeClubId]
            val awayClub = clubsMap[m.awayClubId]
            val homeName = homeClub?.name ?: m.homeClubId
            val awayName = awayClub?.name ?: m.awayClubId
            val homePlayer = playersMap[m.homePlayerId]?.displayName ?: "P${m.homePlayerId}"
            val awayPlayer = playersMap[m.awayPlayerId]?.displayName ?: "P${m.awayPlayerId}"

            MatchCompactCard(
                match = m,
                leftName = homeName,
                rightName = awayName,
                leftPlayer = homePlayer,
                rightPlayer = awayPlayer,
                leftLogo = homeClub?.logoUrl,
                rightLogo = awayClub?.logoUrl,
                onSubmit = { h, a -> onSubmit(m.id, h, a) },
                onEdit = { h, a -> onEdit(m.id, h, a) },
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}