package br.com.alura.AluraFake.application.usecase.task;

import br.com.alura.AluraFake.api.dto.request.OpenTextTaskCreationRequest;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateOpenTextTaskUseCase implements CreateTaskUseCase<OpenTextTaskCreationRequest> {

    private final CourseTaskService courseTaskService;

    public CreateOpenTextTaskUseCase(CourseTaskService courseTaskService) {
        this.courseTaskService = courseTaskService;
    }

    @Override
    public Type getType() {
        return Type.OPEN_TEXT;
    }

    @Override
    @Transactional
    public void execute(OpenTextTaskCreationRequest request) {
        Task newOpenTextTask = Task.builder()
                .statement(request.statement())
                .order(request.order())
                .build();

        courseTaskService.addTaskToCourseAtPosition(
                request.courseId(),
                newOpenTextTask,
                request.order()
        );

    }
}
