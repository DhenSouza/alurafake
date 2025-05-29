package br.com.alura.AluraFake.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChoiceOptionRequest(
        @NotBlank
        String option,

        @NotNull
        Boolean isCorrect
) {}
