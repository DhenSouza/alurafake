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

        @NotEmpty
        @Size(min = 3, max = 5)
        List<ChoiceOptionRequest> options
) implements ChoiceTaskCreationRequest  {
        public MultipleChoiceTaskCreationRequest {
                if (options == null) {
                        throw new IllegalArgumentException("The list of options cannot be null for multiple choice.");
                }

                long correctCount = options.stream().filter(ChoiceOptionRequest::isCorrect).count();
                long incorrectCount = options.size() - correctCount;

                if (correctCount < 2 || incorrectCount < 1) {
                        throw new IllegalArgumentException("Two or more correct alternatives and at least one incorrect alternative are required for Multiple Choice.");
                }

                TaskOptionValidator.validateUniqueAndStatementComparison(statement, options);
        }
}
