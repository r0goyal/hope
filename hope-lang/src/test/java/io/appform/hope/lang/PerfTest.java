/*
 * Copyright 2022. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.hope.lang;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.core.functions.FunctionRegistry;
import jdk.jfr.consumer.RecordedEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.moditect.jfrunit.EnableEvent;
import org.moditect.jfrunit.JfrEventTest;
import org.moditect.jfrunit.JfrEvents;
import org.moditect.jfrunit.events.ObjectAllocationInNewTLAB;
import org.moditect.jfrunit.events.ObjectAllocationOutsideTLAB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test performance between different constructs
 */
@Slf4j
@JfrEventTest
public class PerfTest {

    final ObjectMapper mapper = new ObjectMapper();
    final FunctionRegistry functionRegistry;
    public JfrEvents jfrEvents = new JfrEvents();

    PerfTest() {
        this.functionRegistry = new FunctionRegistry();
        functionRegistry.discover(Collections.emptyList());
    }

    @Test
    @EnableEvent(ObjectAllocationOutsideTLAB.EVENT_NAME)
    @EnableEvent(ObjectAllocationInNewTLAB.EVENT_NAME)
    public void benchmarkEvalJsonPointerMemory() throws IOException {
        int numIterations = 10_000;
        final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        val jsonNode = mapper.createObjectNode()
                .put("instrument", "UPI")
                .put("providerId", "X")
                .put("providerType", "A")
                .put("providerRole", "RECEIVER");
        val hopeRules = readAllHopeRulesForJsonPointer();
        val evaluatables = hopeRules.stream()
                .map(hopeLangEngine::parse)
                .toList();
        jfrEvents.reset();
        /*
        First run some iterations to ensure all internal static memory allocations have happened
         */
        IntStream.range(0, numIterations)
                .forEach(value -> evaluatables.forEach(evaluatable -> hopeLangEngine.evaluate(evaluatable, jsonNode)));
        jfrEvents.reset();

        IntStream.range(0, numIterations)
                .forEach(value -> evaluatables.forEach(evaluatable -> hopeLangEngine.evaluate(evaluatable, jsonNode)));
        jfrEvents.awaitEvents();

        long memoryAllocated = jfrEvents
                .filter(this::isMemoryAllocationEvent)
                .mapToLong(this::getAllocationSize)
                .sum();
        System.out.println("Total memory allocated: " + memoryAllocated);
        /*
        Each run on an average takes less than 175 KB and more than 160 KB as per the perf test
         */
        assertTrue(memoryAllocated < numIterations * 175 * 1024L);
        assertTrue(memoryAllocated > numIterations * 160 * 1024L);
    }

    @Test
    @EnableEvent(ObjectAllocationOutsideTLAB.EVENT_NAME)
    @EnableEvent(ObjectAllocationInNewTLAB.EVENT_NAME)
    public void benchmarkEvalJsonPathMemory() throws IOException {
        int numIterations = 10_000;
        final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        val jsonNode = mapper.createObjectNode()
                .put("instrument", "UPI")
                .put("providerId", "X")
                .put("providerType", "A")
                .put("providerRole", "RECEIVER");
        val hopeRules = readAllHopeRulesForJsonPath();
        val evaluatables = hopeRules.stream()
                .map(hopeLangEngine::parse)
                .toList();
        jfrEvents.reset();
        /*
        First run some iterations to ensure all internal static memory allocations have happened
         */
        IntStream.range(0, numIterations)
                .forEach(value -> evaluatables.forEach(evaluatable -> hopeLangEngine.evaluate(evaluatable, jsonNode)));
        jfrEvents.reset();

        IntStream.range(0, numIterations)
                .forEach(value -> evaluatables.forEach(evaluatable -> hopeLangEngine.evaluate(evaluatable, jsonNode)));
        jfrEvents.awaitEvents();

        long memoryAllocated = jfrEvents
                .filter(this::isMemoryAllocationEvent)
                .mapToLong(this::getAllocationSize)
                .sum();
        System.out.println("Total memory allocated: " + memoryAllocated);
        /*
        Each run on an average takes less than 175 KB and more than 160 KB as per the perf test
         */
        assertTrue(memoryAllocated < numIterations * 800 * 1024L);
        assertTrue(memoryAllocated > numIterations * 750 * 1024L);
    }

    @ParameterizedTest
    @MethodSource("rules")
    @SneakyThrows
    void testPerf(final String payload) {
        val node = mapper.readTree("{ \"value\": 20, \"string\" : \"Hello\" }");

        final HopeLangEngine hopeLangParser = HopeLangEngine.builder()
                .build();

        val rule = hopeLangParser.parse(payload);

        IntStream.range(0, 10)
                .forEach(times -> {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    IntStream.range(1, 1_000_000)
                            .forEach(i -> hopeLangParser.evaluate(rule, node));
                    log.info("Time taken for one million evals: {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                });
        assertTrue(true);
    }

    private static Stream<Arguments> rules() {
        return Stream.of(
                Arguments.of("\"/value\" > 11" +
                        " && \"/value\" <30" +
                        " && \"/value\" > 19 && \"/value\" < 21" +
                        " && \"/value\" >=20 && \"/value\" < 21" +
                        " && \"/value\" > 11 && \"/value\" <= 20"),
                Arguments.of("\"$.value\" > 11" +
                        " && \"$.value\" <30" +
                        " && \"$.value\" > 19 && \"$.value\" < 21" +
                        " && \"$.value\" >=20 && \"$.value\" < 21" +
                        " && \"$.value\" > 11 && \"$.value\" <= 20"),
                Arguments.of("'/value' > 11" +
                        " && '/value' <30" +
                        " && '/value' > 19 && '/value' < 21" +
                        " && '/value' >=20 && '/value' < 21" +
                        " && '/value' > 11 && '/value' <= 20"),
                Arguments.of("'$.value' > 11" +
                        " && '$.value' <30" +
                        " && '$.value' > 19 && '$.value' < 21" +
                        " && '$.value' >=20 && '$.value' < 21" +
                        " && '$.value' > 11 && '$.value' <= 20")
        );
    }

    private long getAllocationSize(final RecordedEvent recordedEvent) {
        return recordedEvent.getEventType().getName().equals(ObjectAllocationInNewTLAB.EVENT_NAME) ?
                recordedEvent.getLong(ObjectAllocationInNewTLAB.TLAB_SIZE.getName()) :
                recordedEvent.getLong(ObjectAllocationOutsideTLAB.ALLOCATION_SIZE.getName());
    }

    private boolean isMemoryAllocationEvent(final RecordedEvent recordedEvent) {
        val eventName = recordedEvent.getEventType().getName();
        return eventName.equals(ObjectAllocationOutsideTLAB.EVENT_NAME)
                || eventName.equals(ObjectAllocationInNewTLAB.EVENT_NAME);
    }

    private List<String> readAllHopeRulesForJsonPointer() throws IOException {
        return Files.readAllLines(Paths.get("src/test/resources/hope_rules_jsonpointer.txt"));
    }

    private List<String> readAllHopeRulesForJsonPath() throws IOException {
        return Files.readAllLines(Paths.get("src/test/resources/hope_rules_jsonpath.txt"));
    }

}
