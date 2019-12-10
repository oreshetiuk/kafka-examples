package com.oreshetiuk.kafkaexamples.ruleresultaggregator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.*
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerde
import java.util.*


object Producers {

    @JvmStatic
    fun main(args: Array<String>) {
        val mapper = jacksonObjectMapper()
        val ruleResultSerde = JsonSerde(RuleResult::class.java, mapper)

        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        props[ProducerConfig.RETRIES_CONFIG] = 0
        props[ProducerConfig.BATCH_SIZE_CONFIG] = 16384
        props[ProducerConfig.LINGER_MS_CONFIG] = 1
        props[ProducerConfig.BUFFER_MEMORY_CONFIG] = 33554432
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ruleResultSerde.serializer().javaClass

        val rule1 = Rule("rule-1", listOf(Action("action-1"), Action("action-2")))
        val rule2 = Rule("rule-2", listOf(Action("action-3")))
        val candidate = Candidate("transfer-9", listOf(rule1, rule2))

//        sendRuleResult(rule1, candidate, rule2, props)

//        ----

        val actionResultSerde = JsonSerde(ActionResult::class.java, mapper)
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = actionResultSerde.serializer().javaClass

        sendActionResult(props, rule1, candidate, rule2)

    }

    private fun sendActionResult(props: MutableMap<String, Any>, rule1: Rule, candidate: Candidate, rule2: Rule) {
        val templateAR = KafkaTemplate(DefaultKafkaProducerFactory<String, ActionResult>(props), true)

        val actionResults = listOf(
                ActionResult(rule1.actions[0], rule1, candidate, ActionResultStatus.REJECT),
                ActionResult(rule1.actions[1], rule1, candidate, ActionResultStatus.FALLBACK)
//,
//                ActionResult(rule2.actions[0], rule2, candidate, ActionResultStatus.NOT_APPLICABLE)
        )


        actionResults.forEach { templateAR.send("rule-engine.action.result", it.candidate.transferId, it) }
    }

    private fun sendRuleResult(rule1: Rule, candidate: Candidate, rule2: Rule, props: MutableMap<String, Any>) {
        val ruleResults = listOf(
                RuleResult(rule1, candidate, true),
                RuleResult(rule2, candidate, true)
        )

        val templateRR = KafkaTemplate(DefaultKafkaProducerFactory<String, RuleResult>(props), true)

        ruleResults.forEach { templateRR.send("rule-engine.execution.result", it.candidate.transferId, it) }
    }
}