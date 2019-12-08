package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class Candidate(
        val transferId: String = "undefined",
        val rules: List<Rule> = ArrayList()
)