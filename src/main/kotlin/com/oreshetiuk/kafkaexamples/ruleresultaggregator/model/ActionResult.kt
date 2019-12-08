package com.oreshetiuk.kafkaexamples.ruleresultaggregator.model


data class ActionResult (
    var action: Action ?= null,
    var rule: Rule ?= null,
    var candidate: Candidate ?= null,
    var actionResultStatus: ActionResultStatus ?= null
)

enum class ActionResultStatus {
    REJECT, RETRY, NOT_APPLICABLE, FALLBACK
}