package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class AggregatedActionResult(
        var candidate: Candidate = Candidate(),
        val actionResults: MutableList<ActionResult> = ArrayList()

) {
    fun add(candidate: Candidate, actionResult: ActionResult): AggregatedActionResult {
        actionResults.add(actionResult)
        this.candidate = candidate
        return this
    }

    @JsonIgnore
    fun containsActions(actions: List<Action>) : Boolean =
        actions.all { it -> actionResults.map { it.action }.toList().contains(it) }

    @JsonIgnore
    fun getAllActions() = actionResults.map { it.action }.toList()
}