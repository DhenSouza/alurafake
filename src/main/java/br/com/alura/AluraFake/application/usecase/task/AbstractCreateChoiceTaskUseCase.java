package br.com.alura.AluraFake.application.usecase.task;

import br.com.alura.AluraFake.api.dto.request.ChoiceTaskCreationRequest;
import br.com.alura.AluraFake.api.dto.request.TaskCreationRequest;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public abstract class AbstractCreateChoiceTaskUseCase <T extends ChoiceTaskCreationRequest> implements CreateTaskUseCase<T> {

    private final CourseTaskService courseTaskService;

    protected AbstractCreateChoiceTaskUseCase(CourseTaskService courseTaskService) {
        this.courseTaskService = courseTaskService;
    }

    @Override
    @Transactional
    public void execute(T request) {
        Task newTask = Task.builder()
                .statement(request.statement())
                .order(request.order())
                .build();

        List<TaskOption> taskOptions = request.options().stream()
                .map(optionRequest -> TaskOption.builder()
                        .optionText(optionRequest.option())
                        .isCorrect(optionRequest.isCorrect())
                        .build())
                .toList();

        taskOptions.forEach(newTask::addOption);

        courseTaskService.addTaskToCourseAtPosition(
                request.courseId(),
                newTask,
                request.order()
        );
    }
}
