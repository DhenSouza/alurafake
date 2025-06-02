package br.com.alura.AluraFake.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChoiceOptionRequest(
        @NotBlank
        @Size(min = 4, max = 80)
        String option,

        @NotNull
        Boolean isCorrect
) {}
