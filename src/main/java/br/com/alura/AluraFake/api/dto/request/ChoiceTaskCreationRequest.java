package br.com.alura.AluraFake.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface ChoiceTaskCreationRequest extends TaskCreationRequest {

    @Valid
    List<ChoiceOptionRequest> options();
}
