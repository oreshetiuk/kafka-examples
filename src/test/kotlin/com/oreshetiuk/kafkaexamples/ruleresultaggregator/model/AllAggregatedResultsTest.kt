package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

import org.junit.Assert
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class AllAggregatedResultsTest {

    @Test
    fun isAggregated() {
        val rule1 = Rule("rule-1", listOf(Action("action-1"), Action("action-2")))
        val rule2 = Rule("rule-2", listOf(Action("action-3")))
        val candidate = Candidate("transfer-5", listOf(rule1, rule2))

        val actionResults = listOf(
                ActionResult(rule1.actions[0], rule1, candidate, ActionResultStatus.REJECT),
                ActionResult(rule1.actions[1], rule1, candidate, ActionResultStatus.FALLBACK),
                ActionResult(rule2.actions[0], rule2, candidate, ActionResultStatus.NOT_APPLICABLE)
        )

        val aggregatedActionResult = AggregatedActionResult(candidate, actionResults.toMutableList())
        val aggregatedRuleResult = AggregatedRuleResult(candidate,
                listOf(RuleResult(rule1, candidate, true),
                        RuleResult(rule2, candidate, true))
                        .toMutableList())

        Assert.assertTrue(AllAggregatedResults(aggregatedRuleResult, aggregatedActionResult).isAggregated())
    }

    @Test
    fun isAggregatedFalse() {
        val rule1 = Rule("rule-1", listOf(Action("action-1"), Action("action-2")))
        val rule2 = Rule("rule-2", listOf(Action("action-3")))
        val candidate = Candidate("transfer-5", listOf(rule1, rule2))

        val actionResults = listOf(
                ActionResult(rule1.actions[0], rule1, candidate, ActionResultStatus.REJECT),
                ActionResult(rule1.actions[1], rule1, candidate, ActionResultStatus.FALLBACK)
        )

        val aggregatedActionResult = AggregatedActionResult(candidate, actionResults.toMutableList())
        val aggregatedRuleResult = AggregatedRuleResult(candidate,
                listOf(RuleResult(rule1, candidate, true),
                        RuleResult(rule2, candidate, true)).toMutableList())

        Assert.assertFalse(AllAggregatedResults(aggregatedRuleResult, aggregatedActionResult).isAggregated())
    }
}