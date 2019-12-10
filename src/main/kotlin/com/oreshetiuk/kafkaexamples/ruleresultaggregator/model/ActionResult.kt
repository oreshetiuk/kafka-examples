package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model


data class ActionResult (
    var action: Action,
    var rule: Rule,
    var candidate: Candidate,
    var actionResultStatus: ActionResultStatus
)

enum class ActionResultStatus {
    REJECT, RETRY, NOT_APPLICABLE, FALLBACK
}