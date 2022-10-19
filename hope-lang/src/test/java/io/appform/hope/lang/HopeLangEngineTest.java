/*
 * Copyright 2019. Santanu Sinha
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@Slf4j
public class HopeLangEngineTest {

    @Test
    public void runBenchmarks() throws Exception {
        Options options = new OptionsBuilder()
                .include(this.getClass().getName() + ".*")
                .mode(Mode.AverageTime)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(1)
                .threads(1)
                .forks(0)
                .measurementIterations(5)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .build();
        new Runner(options).run();
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        final HopeLangEngine hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        List<Evaluatable> evaluatables;
        final Map<String, String> context = ImmutableMap.of("instrument", "A",
                "providerId", "X",
                "providerType", "Y",
                "providerRole", "Z");
        final JsonNode jsonNode = new ObjectMapper().valueToTree(context);

        @Setup(Level.Trial)
        public void
        initialize() throws IOException {
            List<String> hopeRules = Files.readAllLines(Paths.get("src/test/resources/hope_rules.txt"));
            this.evaluatables = hopeRules.stream()
                    .map(hopeLangEngine::parse)
                    .collect(Collectors.toList());
        }
    }

    @Test
    void testFuncIntFailNoExceptNoNode() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{ \"x\" : true }");
        final HopeLangEngine hopeLangParser = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();

        final Evaluatable operator = hopeLangParser.parse("\"$.x\" == \"true\"");

        //NOTE::THIS IS HOW THE BEHAVIOUR IS FOR EQUALS/NOT_EQUALS:
        //BASICALLY THE NODE WILL EVALUATE TO NULL AND WILL MISMATCH EVERYTHING
        assertFalse(hopeLangParser.evaluate(operator, node));
    }

    @Test
    void testFuncIntFailNoExceptNoNodeSQ() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{ \"x\" : true }");
        final HopeLangEngine hopeLangParser = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();

        final Evaluatable operator = hopeLangParser.parse("\"$.x\" == \"true\"");

        //NOTE::THIS IS HOW THE BEHAVIOUR IS FOR EQUALS/NOT_EQUALS:
        //BASICALLY THE NODE WILL EVALUATE TO NULL AND WILL MISMATCH EVERYTHING
        assertFalse(hopeLangParser.evaluate(operator, node));
    }

    @Test
    void testFuncIntFailNoExceptNoNodeJPtr() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree("{ \"x\" : true }");
        final HopeLangEngine hopeLangParser = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();

        final Evaluatable operator = hopeLangParser.parse("\"$.x\" == \"true\"");

        //NOTE::THIS IS HOW THE BEHAVIOUR IS FOR EQUALS/NOT_EQUALS:
        //BASICALLY THE NODE WILL EVALUATE TO NULL AND WILL MISMATCH EVERYTHING
        assertFalse(hopeLangParser.evaluate(operator, node));
    }

    @Test
    void testBlah() throws Exception {
        val hope
                = HopeLangEngine.builder()
                .registerFunction(Blah.class) //Register class by class
                .build();

        JsonNode node = new ObjectMapper().readTree("{}");
        assertTrue(hope.evaluate("ss.blah() == \"blah\"", node));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkSingleRuleEval(BenchmarkState state) {
        IntStream.range(0, 10_000)
                .forEach(value -> state.evaluatables.forEach(evaluatable -> state.hopeLangEngine.evaluate(evaluatable, state.jsonNode)));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkBulkRuleEval(BenchmarkState state) {
        IntStream.range(0, 10_000)
                .forEach(value -> state.hopeLangEngine.evaluate(state.evaluatables, state.jsonNode));
    }
}