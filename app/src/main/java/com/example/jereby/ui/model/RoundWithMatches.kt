package com.example.jereby.ui.model

import com.example.jereby.data.model.Match
import com.example.jereby.data.model.Round

data class RoundWithMatches(val round: Round, val matches: List<Match>)