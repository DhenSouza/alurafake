package br.com.alura.AluraFake.api.dto.request;

import br.com.alura.AluraFake.api.validation.TaskOptionValidator;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.globalHandler.OptionalInvalidException;
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
        List<ChoiceOptionRequest> options
) implements ChoiceTaskCreationRequest  {
        public SingleChoiceTaskCreationRequest {
                if (options != null && options.stream().filter(ChoiceOptionRequest::isCorrect).count() != 1) {
                        throw new OptionalInvalidException("Only one correct alternative is allowed for Single Choice.");
                }

            assert options != null;
            TaskOptionValidator.validateUniqueAndStatementComparison(statement, options);
        }
}
