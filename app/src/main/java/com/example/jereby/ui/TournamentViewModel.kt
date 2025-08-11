package com.example.jereby.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jereby.data.dao.ClubDao
import com.example.jereby.data.dao.ClubStatsDao
import com.example.jereby.data.dao.PlayerDao
import com.example.jereby.data.dao.PlayerStatsDao
import com.example.jereby.data.dao.TournamentDao
import com.example.jereby.data.model.Club
import com.example.jereby.data.model.ClubStats
import com.example.jereby.data.model.Match
import com.example.jereby.data.model.Player
import com.example.jereby.data.model.PlayerStats
import com.example.jereby.data.model.Round
import com.example.jereby.data.model.Tournament
import com.example.jereby.domain.TournamentRepository
import com.example.jereby.domain.model.ClubSourceKind
import com.example.jereby.ui.model.RoundWithMatches
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TournamentViewModel @Inject constructor(
    private val repo: TournamentRepository,
    clubDao: ClubDao,
    playerDao: PlayerDao,
    playerStatsDao: PlayerStatsDao,
    clubStatsDao: ClubStatsDao,
    tournamentDao: TournamentDao,
) : ViewModel() {

    // private mutable, public read-only
    private val _tournamentId = MutableStateFlow<Long?>(null)
    val tournamentId: StateFlow<Long?> = _tournamentId.asStateFlow()

    val rounds: StateFlow<List<Round>> =
        _tournamentId.filterNotNull()
            .flatMapLatest { repo.rounds(it) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentMatches: StateFlow<List<Match>> =
        rounds.map { it.maxByOrNull { r -> r.indexInTournament } }
            .filterNotNull()
            .flatMapLatest { repo.matches(it.id) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val clubsMap: StateFlow<Map<String, Club>> =
        clubDao.all().map { it.associateBy(Club::id) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val currentRoundTitle: StateFlow<String?> =
        rounds.map { it.maxByOrNull { r -> r.indexInTournament }?.displayName }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val playersMap: StateFlow<Map<Long, Player>> =
        playerDao.allFlow().map { it.associateBy(Player::id) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Champion name once the final is decided (latest round has 1 match with a winner)
    val championName: StateFlow<String?> =
        combine(currentMatches, clubsMap) { matches, clubs ->
            if (matches.size == 1) {
                val w = matches.first().winnerClubId ?: return@combine null
                clubs[w]?.name ?: w
            } else null
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val playerStats: StateFlow<List<PlayerStats>> =
        tournamentId.filterNotNull()
            .flatMapLatest { playerStatsDao.byTournament(it) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val clubStats: StateFlow<List<ClubStats>> =
        tournamentId.filterNotNull()
            .flatMapLatest { clubStatsDao.byTournament(it) }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTournaments: StateFlow<List<Tournament>> =
        tournamentDao.list().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val roundsWithMatches: StateFlow<List<RoundWithMatches>> =
        rounds.flatMapLatest { rs ->
            if (rs.isEmpty()) flowOf(emptyList())
            else {
                // combine matches flows for every round
                val flows = rs.map { r -> repo.matches(r.id).map { ms -> r to ms } }
                combine(flows) { array: Array<Pair<Round, List<Match>>> ->
                    array.sortedBy { it.first.indexInTournament }
                        .map { (r, ms) -> RoundWithMatches(r, ms.sortedBy { it.position }) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadExisting(id: Long) {
        if (_tournamentId.value != id) _tournamentId.value = id
    }

    fun create(title: String, players: List<String>, clubCount: Int, source: ClubSourceKind) =
        viewModelScope.launch {
            val id = repo.createTournament(title, players, clubCount, source)
        _tournamentId.value = id
    }

    suspend fun createAndReturnId(
        title: String,
        players: List<String>,
        clubCount: Int,
        source: ClubSourceKind,
    ): Long {
        val id = repo.createTournament(title, players, clubCount, source)
        _tournamentId.value = id
        return id
    }

    fun submitScore(matchId: Long, home: Int, away: Int) = viewModelScope.launch {
        repo.submitScore(matchId, home, away)
    }

    fun editScore(matchId: Long, home: Int, away: Int) = viewModelScope.launch {
        repo.editScore(matchId, home, away)
    }

    fun deleteTournament(tournamentId: Long) = viewModelScope.launch {
        repo.deleteTournament(tournamentId)
    }

}
