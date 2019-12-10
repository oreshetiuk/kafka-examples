package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

data class AllAggregatedResults(
        val aggregatedRuleResult: AggregatedRuleResult?,
        val aggregatedActionResult: AggregatedActionResult?) {

    fun isAggregated(): Boolean {
        val res = if (aggregatedRuleResult == null || aggregatedActionResult == null) false
        else aggregatedActionResult.containsActions(aggregatedRuleResult.getAllActionsOfMatchedRules())
        println("$this IS_AGGREGATED=$res")
        return res
    }

}