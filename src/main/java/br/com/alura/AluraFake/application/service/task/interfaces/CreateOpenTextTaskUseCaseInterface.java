package br.com.alura.AluraFake.application.service.task.interfaces;

import br.com.alura.AluraFake.api.dto.request.TaskRequest;

public interface CreateOpenTextTaskUseCaseInterface {
    void createOpenTextTask(TaskRequest taskRequest);
}
