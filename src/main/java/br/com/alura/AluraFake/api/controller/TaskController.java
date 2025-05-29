package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.application.factory.TaskUseCaseFactory;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task")
public class TaskController {

    private final TaskUseCaseFactory useCaseFactory;

    public TaskController(TaskUseCaseFactory useCaseFactory) {
        this.useCaseFactory = useCaseFactory;
    }

    /* TODO
     * Ajustei o corpo da requisição em um único endpoint para aproveitar o enum
     * e centralizar a lógica de criação de Tasks com suas responsabilidades:
     *   1. Passar o tipo de tarefa via enum.
     *   2. Usar uma factory para automatizar a instância de cada Task.
     *   3. Aplicar o padrão Strategy para definir comportamentos específicos de cada tipo de Task.
     *
     * Dessa forma, não será necessário criar um endpoint exclusivo para cada nova Task:
     * basta adicionar o caso de uso e registrar a Task correspondente no enum.
     */
    @PostMapping("/new")
    public ResponseEntity<?> createNewTask(@Valid @RequestBody TaskCreationRequest request) {
        CreateTaskUseCase<TaskCreationRequest> useCase = useCaseFactory.getUseCase(request.type());

        useCase.execute(request);

        return ResponseEntity.ok().build();
    }
}