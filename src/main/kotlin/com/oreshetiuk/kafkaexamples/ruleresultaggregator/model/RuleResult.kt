package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class RuleResult(
        val rule: Rule,
        val candidate: Candidate,
        val matched: Boolean
)