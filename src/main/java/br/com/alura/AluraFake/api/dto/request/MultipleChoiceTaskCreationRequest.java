package br.com.alura.AluraFake.api.dto.request;

import br.com.alura.AluraFake.api.validation.TaskOptionValidator;
import br.com.alura.AluraFake.domain.enumeration.Type;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record MultipleChoiceTaskCreationRequest(
        @NotNull
        Long courseId,

        @NotNull
        String statement,

        @Min(value = 1)
        @NotNull
        Integer order,

        @NotNull
        Type type,

        List<ChoiceOptionRequest> options
) implements ChoiceTaskCreationRequest  {
        public MultipleChoiceTaskCreationRequest {
                if (options != null && options.stream().noneMatch(ChoiceOptionRequest::isCorrect)) {
                        throw new IllegalArgumentException("Pelo menos uma alternativa correta é necessária para Multiple Choice.");
                }

                TaskOptionValidator.validateUniqueAndStatementComparison(statement, options);
        }
}
