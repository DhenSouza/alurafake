package br.com.alura.AluraFake.api.controller;

import br.com.alura.AluraFake.api.dto.request.TaskRequest;
import br.com.alura.AluraFake.application.service.task.interfaces.CreateOpenTextTaskUseCaseInterface;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/task/new")
public class TaskController {

    private final CreateOpenTextTaskUseCaseInterface createOpenTextTaskUseCase;

    @Autowired
    public TaskController(CreateOpenTextTaskUseCaseInterface createOpenTextTaskUseCase) {
        this.createOpenTextTaskUseCase = createOpenTextTaskUseCase;
    }

    @PostMapping("/opentext")
    public ResponseEntity<?> newOpenTextExercise(@Valid @RequestBody TaskRequest request) {
        this.createOpenTextTaskUseCase.createOpenTextTask(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/singlechoice")
    public ResponseEntity<?> newSingleChoice() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/multiplechoice")
    public ResponseEntity<?> newMultipleChoice() {
        return ResponseEntity.ok().build();
    }

}