package br.com.alura.AluraFake.application.usecase.task;

import br.com.alura.AluraFake.api.dto.request.SingleChoiceTaskCreationRequest;
import br.com.alura.AluraFake.application.interfaces.CreateTaskUseCase;
import br.com.alura.AluraFake.application.service.task.CourseTaskService;
import br.com.alura.AluraFake.domain.enumeration.Type;
import br.com.alura.AluraFake.domain.model.Task;
import br.com.alura.AluraFake.domain.model.TaskOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CreateSingleChoiceUseCase implements CreateTaskUseCase<SingleChoiceTaskCreationRequest> {

    private final CourseTaskService courseTaskService;

    public CreateSingleChoiceUseCase(CourseTaskService courseTaskService) {
        this.courseTaskService = courseTaskService;
    }

    @Override
    public Type getType() {
        return Type.SINGLE_CHOICE;
    }

    @Override
    @Transactional
    public void execute(SingleChoiceTaskCreationRequest request) {
        Task newSingleChoiceTask = Task.builder()
                .statement(request.statement())
                .order(request.order())
                .build();

        List<TaskOption> taskOptions = request.options().stream()
                .map(optionRequest -> TaskOption.builder()
                        .optionText(optionRequest.option())
                        .isCorrect(optionRequest.isCorrect())
                        .build())
                .toList();

        taskOptions.forEach(newSingleChoiceTask::addOption);

        courseTaskService.addTaskToCourseAtPosition(
                request.courseId(),
                newSingleChoiceTask,
                request.order()
        );

    }
}
