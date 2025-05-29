package br.com.alura.AluraFake.api.dto.request;

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
        @Size(min = 2)
        @Valid
        List<ChoiceOptionRequest> options
) implements TaskCreationRequest {
}
