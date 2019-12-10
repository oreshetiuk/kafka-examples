package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class AggregatedRuleResult(
        var candidate: Candidate = Candidate(),
        val matchedRules: MutableList<RuleResult> = ArrayList()
) {

    fun add(candidate: Candidate, ruleResult: RuleResult): AggregatedRuleResult {
        this.candidate = candidate
        matchedRules.add(ruleResult)
        return this
    }

    @JsonIgnore
    fun getAllActionsOfMatchedRules() : List<Action> {
       return matchedRules.filter { it.matched }.flatMap { it.rule.actions }
    }
}