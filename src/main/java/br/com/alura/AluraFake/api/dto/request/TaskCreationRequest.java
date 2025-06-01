package br.com.alura.AluraFake.api.dto.request;

import br.com.alura.AluraFake.domain.enumeration.Type;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OpenTextTaskCreationRequest.class, name = "OPEN_TEXT"),
        @JsonSubTypes.Type(value = SingleChoiceTaskCreationRequest.class, name = "SINGLE_CHOICE"),
        @JsonSubTypes.Type(value = MultipleChoiceTaskCreationRequest.class, name = "MULTIPLE_CHOICE")
})
public interface TaskCreationRequest {

    @NotNull
    Long courseId();

    @Size(min = 4, max = 255, message = "The statement must be between 4 and 255 characters.")
    @NotNull
    String statement();

    @Min(value = 1)
    @NotNull
    Integer order();

    @NotNull
    Type type();
}

