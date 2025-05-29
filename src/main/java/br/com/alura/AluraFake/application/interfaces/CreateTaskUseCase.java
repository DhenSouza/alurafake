package br.com.alura.AluraFake.application.interfaces;

import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.domain.enumeration.Type;

public interface CreateTaskUseCase<R extends TaskCreationRequest> {
    Type getType();
    void execute(R request);
}
