package br.com.alura.AluraFake.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record TaskRequest(

        @NotNull Integer courseId,
        @Length(min = 4, max = 255) String statement,
        @NotNull @Min(0) Integer order
) {
}
