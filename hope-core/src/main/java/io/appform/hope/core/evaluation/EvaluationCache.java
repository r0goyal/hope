package io.appform.hope.core.evaluation;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
public class EvaluationCache {

    private final Map<String, Boolean> evaluationResultMap = new HashMap<>();

    public final static EvaluationCache EMPTY_CACHE = new EvaluationCache();

    public void add(final String evaluatable,
                    final Boolean evaluationResult) {
        evaluationResultMap.put(evaluatable, evaluationResult);
    }

    public Optional<Boolean> get(final String evaluatable) {
        return Optional.ofNullable(evaluationResultMap.get(evaluatable));
    }

}
