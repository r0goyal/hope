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

package io.appform.hope.core.visitors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.*;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.Visitor;
import io.appform.hope.core.VisitorAdapter;
import io.appform.hope.core.combiners.AndCombiner;
import io.appform.hope.core.combiners.OrCombiner;
import io.appform.hope.core.evaluation.EvaluationCache;
import io.appform.hope.core.evaluation.EvaluationResult;
import io.appform.hope.core.exceptions.errorstrategy.DefaultErrorHandlingStrategy;
import io.appform.hope.core.exceptions.errorstrategy.ErrorHandlingStrategy;
import io.appform.hope.core.operators.And;
import io.appform.hope.core.operators.Equals;
import io.appform.hope.core.operators.Greater;
import io.appform.hope.core.operators.GreaterEquals;
import io.appform.hope.core.operators.Lesser;
import io.appform.hope.core.operators.LesserEquals;
import io.appform.hope.core.operators.Not;
import io.appform.hope.core.operators.NotEquals;
import io.appform.hope.core.operators.Or;
import io.appform.hope.core.utils.Converters;
import io.appform.hope.core.values.ArrayValue;
import io.appform.hope.core.values.BooleanValue;
import io.appform.hope.core.values.FunctionValue;
import io.appform.hope.core.values.JsonPathValue;
import io.appform.hope.core.values.JsonPointerValue;
import io.appform.hope.core.values.NumericValue;
import io.appform.hope.core.values.ObjectValue;
import io.appform.hope.core.values.StringValue;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.val;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Evaluates a hope expression
 */
public class Evaluator {
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private final ParseContext parseContext;
    @Getter
    private final ErrorHandlingStrategy errorHandlingStrategy;

    public Evaluator() {
        this(new DefaultErrorHandlingStrategy());
    }

    public Evaluator(ErrorHandlingStrategy errorHandlingStrategy) {
        this.errorHandlingStrategy = errorHandlingStrategy;
        parseContext = JsonPath.using(Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build());

    }

    public boolean evaluate(Evaluatable evaluatable, JsonNode node) {
        return evaluate(evaluatable, node, EvaluationCache.EMPTY_CACHE);
    }

    public boolean evaluate(Evaluatable evaluatable, JsonNode node, EvaluationCache evaluationCache) {
        return evaluatable.accept(new LogicEvaluator(new EvaluationContext(parseContext.parse(node), node, this), evaluationCache));
    }

    public List<Boolean> evaluate(List<Evaluatable> evaluatables, JsonNode node) {
        val tokens = captureAllTokens(evaluatables);
        val evaluationCache = new EvaluationCache();
        tokens.forEach((key, evaluatable) -> {
            boolean result = evaluate(evaluatable, node);
            evaluationCache.add(key,
                    EvaluationResult.builder()
                            .matched(result)
                            .build());
        });
        return evaluatables.stream()
                .map(evaluatable -> evaluate(evaluatable, node, evaluationCache))
                .collect(Collectors.toList());
    }

    private Map<String, Evaluatable> captureAllTokens(List<Evaluatable> evaluatables) {
        Map<String, Evaluatable> tokens = new HashMap<>();
        evaluatables.forEach(evaluatable -> evaluatable.accept(new Visitor<Void>() {

            @Override
            public Void visit(AndCombiner andCombiner) {
                tokens.putAll(captureAllTokens(andCombiner.getExpressions()));
                return null;
            }

            @Override
            public Void visit(OrCombiner orCombiner) {
                tokens.putAll(captureAllTokens(orCombiner.getExpressions()));
                return null;
            }

            @Override
            public Void visit(And and) {
//                tokens.put(and.toString(), and);
                return null;
            }

            @Override
            public Void visit(Equals equals) {
                tokens.put(equals.string(), equals);
                return null;
            }

            @Override
            public Void visit(Greater greater) {
//                tokens.put(greater.toString(), greater);
                return null;
            }

            @Override
            public Void visit(GreaterEquals greaterEquals) {
//                tokens.put(greaterEquals.toString(), greaterEquals);
                return null;
            }

            @Override
            public Void visit(Lesser lesser) {
//                tokens.put(lesser.toString(), lesser);
                return null;
            }

            @Override
            public Void visit(LesserEquals lesserEquals) {
//                tokens.put(lesserEquals.toString(), lesserEquals);
                return null;
            }

            @Override
            public Void visit(NotEquals notEquals) {
//                tokens.put(notEquals.toString(), notEquals);
                return null;
            }

            @Override
            public Void visit(Or or) {
//                tokens.put(or.toString(), or);
                return null;
            }

            @Override
            public Void visit(JsonPathValue jsonPathValue) {
                return null;
            }

            @Override
            public Void visit(JsonPointerValue jsonPointerValue) {
                return null;
            }

            @Override
            public Void visit(ObjectValue objectValue) {
                return null;
            }

            @Override
            public Void visit(NumericValue numericValue) {
                return null;
            }

            @Override
            public Void visit(StringValue stringValue) {
                return null;
            }

            @Override
            public Void visit(BooleanValue booleanValue) {
                return null;
            }

            @Override
            public Void visit(Not not) {
//                tokens.put(not.toString(), not);
                return null;
            }

            @Override
            public Void visit(FunctionValue functionValue) {
                return null;
            }

            @Override
            public Void visit(ArrayValue arrayValue) {
                return null;
            }
        }));
        return tokens;
    }

    @Data
    @Builder
    public static class EvaluationContext {
        private final DocumentContext jsonContext;
        private final JsonNode rootNode;
        private final Evaluator evaluator;
        private final Map<String, JsonNode> jsonPathEvalCache = new HashMap<>(128);
        private final Map<String, JsonNode> jsonPointerEvalCache = new HashMap<>(128);
    }

    public static class LogicEvaluator extends VisitorAdapter<Boolean> {

        private final EvaluationContext evaluationContext;
        private final EvaluationCache evaluationCache;

        public LogicEvaluator(
                EvaluationContext evaluationContext,
                EvaluationCache evaluationCache) {
            super(() -> true);
            this.evaluationContext = evaluationContext;
            this.evaluationCache = evaluationCache;
        }

        public boolean evaluate(Evaluatable evaluatable) {
            return evaluatable.accept(this);
        }

        @Override
        public Boolean visit(AndCombiner andCombiner) {
            return andCombiner.getExpressions()
                    .stream()
                    .allMatch(expression -> expression.accept(new LogicEvaluator(evaluationContext, evaluationCache)));
        }

        @Override
        public Boolean visit(OrCombiner orCombiner) {
            return orCombiner.getExpressions()
                    .stream()
                    .anyMatch(expression -> expression.accept(new LogicEvaluator(evaluationContext, evaluationCache)));
        }

        @Override
        public Boolean visit(Equals equals) {
            val evaluationResult = evaluationCache.get(equals.string()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Object lhs = Converters.objectValue(evaluationContext, equals.getLhs(), null);
            final Object rhs = Converters.objectValue(evaluationContext, equals.getRhs(), null);
            return Objects.equals(lhs, rhs);
        }

        @Override
        public Boolean visit(NotEquals notEquals) {
            val evaluationResult = evaluationCache.get(notEquals.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Object lhs = Converters.objectValue(evaluationContext, notEquals.getLhs(), null);
            final Object rhs = Converters.objectValue(evaluationContext, notEquals.getRhs(), null);
            return !Objects.equals(lhs, rhs);
        }

        @Override
        public Boolean visit(Greater greater) {
            val evaluationResult = evaluationCache.get(greater.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Number lhs = Converters.numericValue(evaluationContext, greater.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, greater.getRhs(), 0);
            return lhs.doubleValue() > rhs.doubleValue();
        }

        @Override
        public Boolean visit(GreaterEquals greaterEquals) {
            val evaluationResult = evaluationCache.get(greaterEquals.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Number lhs = Converters.numericValue(evaluationContext, greaterEquals.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, greaterEquals.getRhs(), 0);
            return lhs.doubleValue() >= rhs.doubleValue();
        }

        @Override
        public Boolean visit(Lesser lesser) {
            val evaluationResult = evaluationCache.get(lesser.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Number lhs = Converters.numericValue(evaluationContext, lesser.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, lesser.getRhs(), 0);
            return lhs.doubleValue() < rhs.doubleValue();
        }

        @Override
        public Boolean visit(LesserEquals lesserEquals) {
            val evaluationResult = evaluationCache.get(lesserEquals.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            final Number lhs = Converters.numericValue(evaluationContext, lesserEquals.getLhs(), 0);
            final Number rhs = Converters.numericValue(evaluationContext, lesserEquals.getRhs(), 0);
            return lhs.doubleValue() <= rhs.doubleValue();
        }

        @Override
        public Boolean visit(And and) {
            boolean lhs = Converters.booleanValue(evaluationContext, and.getLhs(), false);
            boolean rhs = Converters.booleanValue(evaluationContext, and.getRhs(), false);

            return lhs && rhs;
        }

        @Override
        public Boolean visit(Or or) {
            val evaluationResult = evaluationCache.get(or.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            boolean lhs = Converters.booleanValue(evaluationContext, or.getLhs(), false);
            boolean rhs = Converters.booleanValue(evaluationContext, or.getRhs(), false);

            return lhs || rhs;
        }

        @Override
        public Boolean visit(Not not) {
            val evaluationResult = evaluationCache.get(not.toString()).orElse(null);
            if (evaluationResult != null){
                return evaluationResult.isMatched();
            }
            boolean operand = Converters.booleanValue(evaluationContext, not.getOperand(), false);
            return !operand;
        }

        @Override
        public Boolean visit(JsonPointerValue jsonPointerValue) {
            return null;
        }

    }

}
