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

    @PostMapping("/new")
    public ResponseEntity<?> createNewTask(@Valid @RequestBody TaskCreationRequest request) {
        CreateTaskUseCase<TaskCreationRequest> useCase = useCaseFactory.getUseCase(request.type());

        useCase.execute(request);

        return ResponseEntity.ok().build();
    }
}