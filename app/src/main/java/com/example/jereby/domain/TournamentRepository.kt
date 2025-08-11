// domain/TournamentRepository.kt
package com.example.jereby.domain

import androidx.room.withTransaction
import com.example.jereby.data.*
import com.example.jereby.data.dao.*
import kotlinx.coroutines.flow.Flow
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TournamentRepository @Inject constructor(
    private val db: AppDatabase,
    private val clubDao: ClubDao,
    private val playerDao: PlayerDao,
    private val tournamentDao: TournamentDao,
    private val roundDao: RoundDao,
    private val matchDao: MatchDao,
    private val localClubSource: LocalClubSource,
    private val playerStatsDao: PlayerStatsDao,
    private val clubStatsDao: ClubStatsDao
) {

    // --- Creation & queries ---------------------------------------------------

    suspend fun createTournament(
        title: String,
        playerNames: List<String>,
        clubCount: Int,
        rnd: Random = Random()
    ): Long {
        require(clubCount % 2 == 0) { "Club count must be even" }

        val tid = tournamentDao.insert(Tournament(title = title))
        val playerIds = playerDao.insertAll(playerNames.map { Player(displayName = it) })

        val clubs = localClubSource.load(limit = clubCount)
        clubDao.upsertAll(clubs)

        val roundId = roundDao.insert(
            Round(tournamentId = tid, indexInTournament = 0, displayName = "Round of $clubCount")
        )

        val shuffled = clubs.shuffled(rnd)
        val matches = shuffled.chunked(2).mapIndexed { idx, pair ->
            val (a, b) = pair
            val aPlayer = playerIds[idx % playerIds.size]
            val bPlayer = playerIds[(idx + 1) % playerIds.size]
            Match(
                roundId = roundId,
                position = idx,
                homeClubId = a.id, awayClubId = b.id,
                homePlayerId = aPlayer, awayPlayerId = bPlayer
            )
        }
        matchDao.insertAll(matches)
        return tid
    }

    fun observeTournament(tournamentId: Long): Flow<Tournament> = tournamentDao.observe(tournamentId)
    fun rounds(tournamentId: Long): Flow<List<Round>> = roundDao.byTournament(tournamentId)
    fun matches(roundId: Long): Flow<List<Match>> = matchDao.byRound(roundId)

    // --- Match results & advancement -----------------------------------------

    suspend fun submitScore(matchId: Long, home: Int, away: Int) {
        db.withTransaction {
            val m = getMatch(matchId) ?: return@withTransaction
            if (m.winnerClubId != null) return@withTransaction  // idempotent

            val winnerClubId = if (home > away) m.homeClubId else m.awayClubId
            matchDao.update(
                m.copy(homeScore = home, awayScore = away, winnerClubId = winnerClubId)
            )

            val tid = tournamentIdForRound(m.roundId) ?: return@withTransaction
            updatePlayerStats(tid, m.homePlayerId, home, away, home > away)
            updatePlayerStats(tid, m.awayPlayerId, away, home, away > home)
            updateClubStats(tid, m.homeClubId, home, away, home > away)
            updateClubStats(tid, m.awayClubId, away, home, away > home)

            advanceIfRoundComplete(m.roundId)
        }
    }

    // --- Helpers (private) ----------------------------------------------------

    private suspend fun getMatch(matchId: Long): Match? =
        matchDao.byIds(listOf(matchId)).firstOrNull()

    private suspend fun tournamentIdForRound(roundId: Long): Long? =
        roundDao.get(roundId)?.tournamentId

    private suspend fun updatePlayerStats(
        tournamentId: Long,
        playerId: Long,
        goalsFor: Int,
        goalsAgainst: Int,
        win: Boolean
    ) {
        val current = playerStatsDao.get(tournamentId, playerId) ?: PlayerStats(tournamentId, playerId)
        playerStatsDao.upsert(
            current.copy(
                wins = current.wins + if (win) 1 else 0,
                losses = current.losses + if (!win) 1 else 0,
                goalsFor = current.goalsFor + goalsFor,
                goalsAgainst = current.goalsAgainst + goalsAgainst
            )
        )
    }

    private suspend fun updateClubStats(
        tournamentId: Long,
        clubId: String,
        goalsFor: Int,
        goalsAgainst: Int,
        win: Boolean
    ) {
        val current = clubStatsDao.get(tournamentId, clubId) ?: ClubStats(tournamentId, clubId)
        clubStatsDao.upsert(
            current.copy(
                wins = current.wins + if (win) 1 else 0,
                losses = current.losses + if (!win) 1 else 0,
                goalsFor = current.goalsFor + goalsFor,
                goalsAgainst = current.goalsAgainst + goalsAgainst
            )
        )
    }

    private suspend fun advanceIfRoundComplete(roundId: Long) {
        val round = roundDao.get(roundId) ?: return
        val matches = matchDao.listOnce(roundId)
        if (matches.any { it.winnerClubId == null }) return

        val winners = matches.mapNotNull { it.winnerClubId }
        val tid = round.tournamentId

        if (winners.size == 1) {
            // Tournament finished
            tournamentDao.get(tid)?.let { tournamentDao.update(it.copy(status = "FINISHED")) }
            return
        }

        val clubsById = clubDao.allOnce().associateBy { it.id }
        val winnerClubs = winners.mapNotNull { clubsById[it] }

        val nextIndex = round.indexInTournament + 1
        val nextName = when (winnerClubs.size) {
            16 -> "Round of 16"
            8  -> "Quarterfinals"
            4  -> "Semifinals"
            2  -> "Final"
            else -> "Round of ${winnerClubs.size}"
        }
        val nextRoundId = roundDao.insert(
            Round(tournamentId = tid, indexInTournament = nextIndex, displayName = nextName)
        )

        val players = playerDao.allOnce()
        val rnd = Random()
        val shuffled = winnerClubs.shuffled(rnd)
        val newMatches = shuffled.chunked(2).mapIndexed { idx, pair ->
            val (a, b) = pair
            val aPlayer = players[idx % players.size].id
            val bPlayer = players[(idx + 1) % players.size].id
            Match(
                roundId = nextRoundId,
                position = idx,
                homeClubId = a.id, awayClubId = b.id,
                homePlayerId = aPlayer, awayPlayerId = bPlayer
            )
        }
        matchDao.insertAll(newMatches)
    }
}
