package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model

import org.junit.Assert
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import javax.validation.constraints.AssertTrue

internal class AggregatedActionResultTest {

    @Test
    fun testContainsActions() {
//        AggregatedRuleResult(
//                Candidate("transfer-7",
//                        [Rule("rule-1", [Action("action-1"), Action("action-2")].toList()),
//                            Rule("rule-2", [Action("action-3")])].toList()),
//                [RuleResult(Rule("rule-1", [Action("action-1"), Action("action-2")]),
//                        Candidate("transfer-7", [Rule(id = "rule-1",
//                                [Action("action-1"), Action("action-2")]),
//                            Rule("rule-2", [Action("action-3")])]), true),
//                    RuleResult(Rule("rule - 2", [Action(id = action - 3)]), candidate = Candidate(transferId = transfer - 7, rules = [Rule(id = rule - 1, actions = [Action(id = action - 1), Action(id = action - 2)]), Rule(id = rule - 2, actions = [Action(id = action - 3)])]), matched = true)]), aggregatedActionResult = AggregatedActionResult(candidate = Candidate(transferId = transfer-7, rules = [Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), Rule(id = rule-2, actions = [Action(id = action-3)])]), actionResults = [ActionResult(action = Action(id = action-3), rule = Rule(id = rule-2, actions = [Action(id = action-3)]), candidate = Candidate(transferId = transfer-7, rules = [Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), Rule(id = rule-2, actions = [Action(id = action-3)])]), actionResultStatus = NOT_APPLICABLE), ActionResult(action = Action(id = action-1), rule = Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), candidate = Candidate(transferId = transfer-7, rules = [Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), Rule(id = rule-2, actions = [Action(id = action-3)])]), actionResultStatus = REJECT), ActionResult(action = Action(id = action-2), rule = Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), candidate = Candidate(transferId = transfer-7, rules = [Rule(id = rule-1, actions = [Action(id = action-1), Action(id = action-2)]), Rule(id = rule-2, actions = [Action(id = action-3)])]), actionResultStatus = FALLBACK)])

        val rule1 = Rule("rule-1", listOf(Action("action-1"), Action("action-2")))
        val rule2 = Rule("rule-2", listOf(Action("action-3")))
        val candidate = Candidate("transfer-5", listOf(rule1, rule2))

        val actionResults = listOf(
                ActionResult(rule1.actions[0], rule1, candidate, ActionResultStatus.REJECT),
                ActionResult(rule1.actions[1], rule1, candidate, ActionResultStatus.FALLBACK),
                ActionResult(rule2.actions[0], rule2, candidate, ActionResultStatus.NOT_APPLICABLE)
        )

        val aggregatedActionResult = AggregatedActionResult(candidate, actionResults.toMutableList())

        Assert.assertTrue(aggregatedActionResult
                .containsActions(listOf(Action("action-1"), Action("action-2"), Action("action-3"))))

        Assert.assertTrue(aggregatedActionResult
                .containsActions(listOf(Action("action-2"), Action("action-3"), Action("action-1"))))

        Assert.assertFalse(aggregatedActionResult
                .containsActions(listOf(Action("action-1"), Action("action-2"))))

        Assert.assertFalse(aggregatedActionResult
                .containsActions(listOf(Action("action-1"))))
    }

}