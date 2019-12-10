package com.oreshetiuk.kafkaexamples.ruleresultaggregator

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonSerde
import java.time.Duration
import java.util.function.BiFunction
import java.util.function.Function


@Configuration
internal class RuleResultAggregatorStreamsConfiguration {
    @Autowired
    private val interactiveQueryService: InteractiveQueryService? = null
    private val mapper = jacksonObjectMapper()
    private val ruleResultSerde = JsonSerde(RuleResult::class.java, mapper)
    private val aggregatedRuleResultSerde = JsonSerde(AggregatedRuleResult::class.java, mapper)

    private val actionResultSerde = JsonSerde(ActionResult::class.java, mapper)
    private val aggregatedActionResultSerde = JsonSerde(AggregatedActionResult::class.java, mapper)


    @Bean
    fun aggregateRuleResult() =
            Function<KStream<String, RuleResult>, KStream<String, AggregatedRuleResult>> { ruleResult ->
                ruleResult
                        .groupBy(transferId(), Grouped.with(Serdes.String(), ruleResultSerde))
                        .aggregate({ AggregatedRuleResult() }, addToAggregatedRuleResult(), materializeAs("ktable-rule-aggregated-result"))
                        .toStream()
                        .filter { key, value -> containsAllRuleResult("ktable-rule-aggregated-result", key, value) }
            }

    private fun transferId() = { _: String, ruleResult: RuleResult -> ruleResult.candidate.transferId }

    private fun materializeAs(storeName: String) =
            Materialized.`as`<String, AggregatedRuleResult, KeyValueStore<Bytes, ByteArray>>(storeName)
                    .withKeySerde(Serdes.String()).withValueSerde(aggregatedRuleResultSerde)

    private fun addToAggregatedRuleResult() = { _: String, ruleResult: RuleResult, aggregatedRuleResult: AggregatedRuleResult ->
        aggregatedRuleResult.add(ruleResult.candidate, ruleResult)
    }

    private fun addToAggregatedActionResult() = { _: String, actionResult: ActionResult, aggregatedActionResult: AggregatedActionResult ->
        aggregatedActionResult.add(actionResult.candidate, actionResult)
    }

    private fun materializeActionResultAs(storeName: String) =
            Materialized.`as`<String, AggregatedActionResult, KeyValueStore<Bytes, ByteArray>>(storeName)
                    .withKeySerde(Serdes.String()).withValueSerde(aggregatedActionResultSerde)


    @Bean
    fun aggregateActionResult() = BiFunction<
            KStream<String, ActionResult>,
            KStream<String, AggregatedRuleResult>,
            KStream<String, AggregatedActionResult>>

    { actionResultStream, aggregatedRuleResultStream ->

        val kTableAggActionResult: KTable<String, AggregatedActionResult> = actionResultStream
                .groupBy({ _: String, actionResult: ActionResult -> actionResult.candidate.transferId }, Grouped.with(Serdes.String(), actionResultSerde))
                .aggregate({ AggregatedActionResult() }, addToAggregatedActionResult(), materializeActionResultAs("ktable-action-aggregated-result"))

        val kStreamAggActionResult = kTableAggActionResult.toStream()

        kStreamAggActionResult
                .leftJoin(
                        aggregatedRuleResultStream,
                        { aggregatedActionResult, aggregatedRuleResult ->
                            println("join is invoked actionResult=$aggregatedActionResult aggregatedRuleResult=$aggregatedRuleResult")
                            AllAggregatedResults(aggregatedRuleResult, aggregatedActionResult)
                        },
                        JoinWindows.of(Duration.ofHours(2)),
                        Joined.with(Serdes.String(), aggregatedActionResultSerde, aggregatedRuleResultSerde)
                )
                .filter { _, allAggrResults: AllAggregatedResults -> allAggrResults.isAggregated() }
                .mapValues(AllAggregatedResults::aggregatedActionResult)

        aggregatedRuleResultStream
                .leftJoin(
                        kTableAggActionResult,
                        { aggregatedRuleResult, aggregatedActionResult ->
                            println("join is invoked aggregatedRuleResult=$aggregatedRuleResult aggregatedActionResult=$aggregatedActionResult")
                            AllAggregatedResults(aggregatedRuleResult, aggregatedActionResult)
                        },
                        Joined.with(Serdes.String(), aggregatedRuleResultSerde, aggregatedActionResultSerde)
                ).filter { _, allAggrResults: AllAggregatedResults -> allAggrResults.isAggregated() }
                .mapValues(AllAggregatedResults::aggregatedActionResult)

    }

    private fun containsAllRuleResult(kTable: String, key: String, value: AggregatedRuleResult): Boolean {
        val store = interactiveQueryService!!.getQueryableStore(kTable, QueryableStoreTypes.keyValueStore<String, AggregatedRuleResult?>())
        val res = store.all()
        println("containsAllRuleResult invoked")
        res.forEach { println("key=${it.key} value=${it.value}") }
        val isAggregated = store[key]?.matchedRules?.size == value.candidate.rules.size
        println(" isAggregated = $isAggregated for transferId = ${value.candidate.transferId}")
        return isAggregated
    }
}