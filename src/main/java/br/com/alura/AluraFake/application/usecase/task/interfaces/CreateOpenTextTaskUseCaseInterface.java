package br.com.alura.AluraFake.application.usecase.task.interfaces;

import br.com.alura.AluraFake.api.dto.request.OpenTextTaskCreationRequest;

public interface CreateOpenTextTaskUseCaseInterface {
    void createOpenTextTask(OpenTextTaskCreationRequest openTextTaskCreationRequest);
}
