package br.com.alura.AluraFake.api.dto.request;

import br.com.alura.AluraFake.domain.enumeration.Type;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenTextTaskCreationRequest(
        @NotNull
        Long courseId,

        @NotBlank
        String statement,

        @Min(value = 1)
        @NotNull
        Integer order,

        @NotNull
        Type type
) implements TaskCreationRequest {}
