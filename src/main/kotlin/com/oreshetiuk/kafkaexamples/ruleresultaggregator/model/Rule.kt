package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class Rule(
    val id: String = "undefined",
    val actions: List<Action> = ArrayList()
)
