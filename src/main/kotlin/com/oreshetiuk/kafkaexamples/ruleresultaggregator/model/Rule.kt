package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class Rule(
    val id: String ?= null,
    val actions: List<Action> ?= null
)
