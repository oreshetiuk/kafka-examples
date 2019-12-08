package com.oreshetiuk.kafkaexamples

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.ActionResult
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.AggregatedActionResult
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.AggregatedRuleResult
import com.oreshetiuk.kafkaexamples.ruleresultaggregator.model.RuleResult
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonSerde
import java.util.function.Function
import java.util.function.Consumer


@Configuration
internal class RuleResultAggregatorStreamsConfiguration {
    @Autowired
    private val interactiveQueryService: InteractiveQueryService? = null
    private val mapper = jacksonObjectMapper()
    private val ruleResultSerde = JsonSerde(RuleResult::class.java, mapper)
    private val aggregatedRuleResult = JsonSerde(AggregatedRuleResult::class.java, mapper)


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
                    .withKeySerde(Serdes.String()).withValueSerde(aggregatedRuleResult)

    private fun addToAggregatedRuleResult() = { _: String, ruleResult: RuleResult, aggregatedRuleResult: AggregatedRuleResult ->
        aggregatedRuleResult.add(ruleResult.candidate, ruleResult)
    }

    @Bean
    fun aggregateActionResult(): Consumer<KStream<String?, ActionResult>> {
        val mapper = ObjectMapper()
        val actionResultSerde = JsonSerde(ActionResult::class.java, mapper)
        val aggregatedActionSerde = JsonSerde(AggregatedActionResult::class.java, mapper)

        return Consumer { input: KStream<String?, ActionResult> ->
            val kTable = input
                    .peek { key, value -> println("Got key=$key value=$value") }
                    .groupBy(
                            { _: String?, actionResult: ActionResult -> actionResult.candidate?.transferId }, Grouped.with(null, actionResultSerde)
                    )
                    .aggregate({ AggregatedActionResult() },
                            { _: String?,
                              actionResult: ActionResult,
                              aggregatedActionResult: AggregatedActionResult ->
                                aggregatedActionResult.add(actionResult.candidate!!, actionResult)
                            },
                            Materialized.`as`<String, AggregatedActionResult, KeyValueStore<Bytes, ByteArray>>("ktable-action-aggregated-result")
                                    .withKeySerde(Serdes.String()).withValueSerde(aggregatedActionSerde)
                    )
            kTable.toStream()
                    .filter { key, value -> containsAllActionResult(kTable.queryableStoreName(), key, value) }
                    .to("rule.engine.action-aggregated-result")
        }
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

    private fun containsAllActionResult(ktableName: String, key: String?, value: AggregatedActionResult): Boolean {
        val storeAggregatedActionResult = interactiveQueryService!!.getQueryableStore(ktableName, QueryableStoreTypes.keyValueStore<String, AggregatedActionResult?>())
        val storeRuleAggregatedResult = interactiveQueryService.getQueryableStore("ktable-rule-aggregated-result", QueryableStoreTypes.keyValueStore<String, AggregatedRuleResult?>())

        val res = storeAggregatedActionResult.all()
        res.forEach { println("key=${it.key} value=${it.value}") }
        val isAggregated = storeAggregatedActionResult[key]?.actionResults?.size == storeRuleAggregatedResult[key]?.getAllActionsMatchedRules()?.size
        println(" isAggregated action results = $isAggregated for transferId = ${value.candidate.transferId}")
        return isAggregated
    }


//
//    private fun containsAllActionResult(transactionId: String): Boolean {
//        val storeRuleAggregatedResult = interactiveQueryService!!.getQueryableStore("ktable-rule-aggregated-result", QueryableStoreTypes.keyValueStore<String, AggregatedActionResult?>())
//        val storeAggregatedActionResult = interactiveQueryService!!.getQueryableStore("ktable-action-aggregated-result", QueryableStoreTypes.keyValueStore<String, AggregatedActionResult?>())
//
//        storeAggregatedActionResult.all().forEach { println("key=${it.key} value=${it.value}") }
//
//        //todo: size is not enough to check
//        val isAggregated = storeAggregatedActionResult[transactionId] != null
//                && storeAggregatedActionResult[transactionId]?.actionResults?.size == storeRuleAggregatedResult[transactionId]?.actionResults.size
//
//        println(" isAggregated = $isAggregated for transferId = ${transactionId}")
//        return isAggregated
//    }


//    @RestController
//    public inner class FooController {
//        @RequestMapping("/events")
//        fun events(): KeyValueIterator<String, String> {
//            val topFiveStore = interactiveQueryService!!.getQueryableStore("test-events-snapshots", QueryableStoreTypes.keyValueStore<String, String>())
//            return topFiveStore.all()
//        }
//    }

}