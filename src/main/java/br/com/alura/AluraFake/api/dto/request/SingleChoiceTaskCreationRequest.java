package br.com.alura.AluraFake.api.dto.request;

import br.com.alura.AluraFake.api.validation.TaskOptionValidator;
import br.com.alura.AluraFake.domain.enumeration.Type;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SingleChoiceTaskCreationRequest(
        @NotNull
        Long courseId,

        @NotNull
        String statement,

        @Min(value = 1)
        @NotNull
        Integer order,

        @NotNull
        Type type,

        @NotEmpty
        @Size(min = 2, max = 5)
        @Valid
        List<ChoiceOptionRequest> options
) implements TaskCreationRequest {
        public SingleChoiceTaskCreationRequest {
                if (options != null && options.stream().filter(ChoiceOptionRequest::isCorrect).count() != 1) {
                        throw new IllegalArgumentException("Uma e apenas uma alternativa correta é permitida para Single Choice.");
                }

            assert options != null;
            TaskOptionValidator.validateUniqueAndStatementComparison(statement, options);
        }
}
