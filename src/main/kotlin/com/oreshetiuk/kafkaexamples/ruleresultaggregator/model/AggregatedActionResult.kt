package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class AggregatedActionResult(
        var candidate: Candidate = Candidate(),
        val actionResults: MutableList<ActionResult> = ArrayList()

) {
    fun add(candidate: Candidate, actionResult: ActionResult): AggregatedActionResult {
        actionResults.add(actionResult)
        this.candidate = candidate
        return this
    }
}