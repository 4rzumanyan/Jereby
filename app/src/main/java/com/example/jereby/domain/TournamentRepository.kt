// domain/TournamentRepository.kt
package com.example.jereby.domain

import androidx.room.withTransaction
import com.example.jereby.data.AppDatabase
import com.example.jereby.data.dao.ClubDao
import com.example.jereby.data.dao.ClubStatsDao
import com.example.jereby.data.dao.MatchDao
import com.example.jereby.data.dao.PlayerDao
import com.example.jereby.data.dao.PlayerStatsDao
import com.example.jereby.data.dao.RoundDao
import com.example.jereby.data.dao.TournamentDao
import com.example.jereby.data.datasource.club.ClubSource
import com.example.jereby.data.model.Club
import com.example.jereby.data.model.ClubStats
import com.example.jereby.data.model.Match
import com.example.jereby.data.model.Player
import com.example.jereby.data.model.PlayerStats
import com.example.jereby.data.model.Round
import com.example.jereby.data.model.Tournament
import com.example.jereby.di.LocalClubs
import com.example.jereby.di.UefaClubs
import com.example.jereby.domain.model.ClubSourceKind
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
    @LocalClubs private val localSource: ClubSource,
    @UefaClubs private val uefaSource: ClubSource,
    private val playerStatsDao: PlayerStatsDao,
    private val clubStatsDao: ClubStatsDao,
) {

    // --- Creation & queries ---------------------------------------------------

    suspend fun createTournament(
        title: String,
        playerNames: List<String>,
        clubCount: Int,
        source: ClubSourceKind = ClubSourceKind.LOCAL,
        rnd: Random = Random(),
    ): Long {
        require(clubCount % 2 == 0) { "Club count must be even" }

        val tid = tournamentDao.insert(Tournament(title = title))
        val playerIds = playerDao.insertAll(playerNames.map { Player(displayName = it) })

        val clubs = loadClubsWithFallback(source, clubCount)
        clubDao.upsertAll(clubs)

        val roundId = roundDao.insert(
            Round(tournamentId = tid, indexInTournament = 0, displayName = "Round of $clubCount")
        )

        val shuffled = clubs.shuffled(rnd)
        val matches = shuffled.chunked(2).mapIndexed { idx, (a, b) ->
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

    private suspend fun loadClubsWithFallback(source: ClubSourceKind, count: Int): List<Club> {
        val primary = when (source) {
            ClubSourceKind.LOCAL -> localSource
            ClubSourceKind.UEFA -> uefaSource
        }
        return try {
            val list = primary.load(count)
            if (list.size >= 2 && list.size % 2 == 0) list else localSource.load(count)
        } catch (_: Exception) {
            localSource.load(count)
        }
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

    suspend fun editScore(matchId: Long, newHome: Int, newAway: Int) {
        db.withTransaction {
            val match = getMatch(matchId) ?: return@withTransaction
            val round = roundDao.get(match.roundId) ?: return@withTransaction
            val tid = round.tournamentId

            val oldHome = match.homeScore
            val oldAway = match.awayScore
            val oldWinner = match.winnerClubId
            val newWinner = if (newHome > newAway) match.homeClubId else match.awayClubId

            // If score and winner unchanged, no action
            if (oldHome == newHome && oldAway == newAway && oldWinner == newWinner) return@withTransaction

            // Revert stats if there was an old result
            if (oldHome != null && oldAway != null && oldWinner != null) {
                revertStats(tid, match, oldHome, oldAway, oldWinner)
            }

            // Update the match
            matchDao.update(
                match.copy(
                    homeScore = newHome,
                    awayScore = newAway,
                    winnerClubId = newWinner
                )
            )

            // Delete future rounds
            deleteFutureRounds(tid, round.indexInTournament)

            // Recompute downstream rounds
            recomputeFromRound(round)
        }
    }

    private suspend fun revertStats(
        tournamentId: Long,
        match: Match,
        oldHome: Int,
        oldAway: Int,
        oldWinner: String,
    ) {
        // Player stats
        adjustPlayerStats(
            tournamentId,
            match.homePlayerId,
            win = (oldWinner == match.homeClubId),
            gf = oldHome,
            ga = oldAway,
            revert = true
        )
        adjustPlayerStats(
            tournamentId,
            match.awayPlayerId,
            win = (oldWinner == match.awayClubId),
            gf = oldAway,
            ga = oldHome,
            revert = true
        )

        // Club stats
        adjustClubStats(
            tournamentId,
            match.homeClubId,
            win = (oldWinner == match.homeClubId),
            gf = oldHome,
            ga = oldAway,
            revert = true
        )
        adjustClubStats(
            tournamentId,
            match.awayClubId,
            win = (oldWinner == match.awayClubId),
            gf = oldAway,
            ga = oldHome,
            revert = true
        )
    }

    private suspend fun adjustPlayerStats(
        tournamentId: Long,
        playerId: Long,
        win: Boolean,
        gf: Int,
        ga: Int,
        revert: Boolean = false,
    ) {
        val cur = playerStatsDao.get(tournamentId, playerId) ?: PlayerStats(tournamentId, playerId)
        playerStatsDao.upsert(
            cur.copy(
                wins = cur.wins + if (revert) -(if (win) 1 else 0) else if (win) 1 else 0,
                losses = cur.losses + if (revert) -(if (!win) 1 else 0) else if (!win) 1 else 0,
                goalsFor = cur.goalsFor + if (revert) -gf else gf,
                goalsAgainst = cur.goalsAgainst + if (revert) -ga else ga
            )
        )
    }

    private suspend fun adjustClubStats(
        tournamentId: Long,
        clubId: String,
        win: Boolean,
        gf: Int,
        ga: Int,
        revert: Boolean = false,
    ) {
        val cur = clubStatsDao.get(tournamentId, clubId) ?: ClubStats(tournamentId, clubId)
        clubStatsDao.upsert(
            cur.copy(
                wins = cur.wins + if (revert) -(if (win) 1 else 0) else if (win) 1 else 0,
                losses = cur.losses + if (revert) -(if (!win) 1 else 0) else if (!win) 1 else 0,
                goalsFor = cur.goalsFor + if (revert) -gf else gf,
                goalsAgainst = cur.goalsAgainst + if (revert) -ga else ga
            )
        )
    }

    private suspend fun deleteFutureRounds(tournamentId: Long, fromIndex: Int) {
        val later = roundDao.laterRounds(tournamentId, fromIndex)
        if (later.isNotEmpty()) {
            val ids = later.map { it.id }
            matchDao.deleteByRoundIds(ids)
            roundDao.deleteRounds(ids)
        }
    }

    private suspend fun recomputeFromRound(startRound: Round) {
        var round = startRound
        while (true) {
            val matches = matchDao.listOnce(round.id)
            if (matches.any { it.winnerClubId == null }) break

            val winners = matches.mapNotNull { it.winnerClubId }
            if (winners.size == 1) {
                tournamentDao.get(round.tournamentId)?.let {
                    tournamentDao.update(it.copy(status = "FINISHED"))
                }
                break
            }

            val clubsById = clubDao.allOnce().associateBy { it.id }
            val winnerClubs = winners.mapNotNull { clubsById[it] }

            val nextIndex = round.indexInTournament + 1
            val nextName = when (winnerClubs.size) {
                16 -> "Round of 16"
                8 -> "Quarterfinals"
                4 -> "Semifinals"
                2 -> "Final"
                else -> "Round of ${winnerClubs.size}"
            }
            val nextRoundId = roundDao.insert(
                Round(
                    tournamentId = round.tournamentId,
                    indexInTournament = nextIndex,
                    displayName = nextName
                )
            )

            val players = playerDao.allOnce()
            val rnd = Random(42) // deterministic assignment
            val shuffled = winnerClubs.shuffled(rnd)
            val newMatches = shuffled.chunked(2).mapIndexed { idx, (a, b) ->
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

            round = roundDao.get(nextRoundId) ?: break
        }
    }

    suspend fun deleteTournament(tournamentId: Long) {
        db.withTransaction {
            val roundIds = roundDao.idsByTournament(tournamentId)
            if (roundIds.isNotEmpty()) {
                matchDao.deleteByRoundIds(roundIds)
            }
            playerStatsDao.deleteByTournament(tournamentId)
            clubStatsDao.deleteByTournament(tournamentId)
            roundDao.deleteByTournament(tournamentId)
            tournamentDao.deleteById(tournamentId)
        }
    }

}
